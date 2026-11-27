"""Bounded, durable musical catalog preparation driven by positive activity."""
import logging
import os
import threading
from pathlib import Path

import httpx
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from ..database import get_connection
from ..music_models import MUSIC_FEATURE_VERSION, MusicRef
from ..music_features_repository import get_music_features, get_music_by_mbid, save_music_features, save_music_alias, list_music_catalog
from ..provider_budget import PreparationDeferred
from .music_discovery import MusicDiscovery, MappingUnavailable

logger = logging.getLogger(__name__)
WORKER_LOCK_ID = 7512052


def enabled():
    return os.environ.get('MUSIC_CATALOG_AUTOPREPARE','true').lower() in {'true','1','yes'}


def enqueue_music(seed_keys):
    if not enabled():
        return {'status':'disabled','pending_count':0,'failed_count':0}
    refs = []
    for key in sorted(set(seed_keys)):
        kind, sep, oid = key.partition(':')
        if sep:
            refs.append(MusicRef(contentType=kind,externalId=oid))
    keys = [ref.key for ref in refs]
    if not keys:
        return {'status':'ready','pending_count':0,'failed_count':0}
    with get_connection() as connection:
        connection.execute('SELECT pg_advisory_xact_lock(7512051)')
        count = connection.execute("SELECT count(*) FROM music_catalog_jobs WHERE status IN ('pending','running','retry')").fetchone()[0]
        existing = {row[0] for row in connection.execute('SELECT DISTINCT ref_key FROM music_catalog_jobs WHERE ref_key=ANY(%s) AND feature_version=%s',(keys,MUSIC_FEATURE_VERSION)).fetchall()}
        admitted = 0
        for ref in sorted(refs,key=lambda value:(value.key in existing,value.key))[:100]:
            saved = get_music_features(ref.contentType,ref.externalId)
            current = bool(saved and saved['feature_version']==MUSIC_FEATURE_VERSION)
            for kind in (['expand'] if current else ['prepare','expand']):
                if count >= 200:
                    break
                row = connection.execute('''INSERT INTO music_catalog_jobs
                    (ref_key,content_type,external_id,kind,feature_version,priority,parent_refs)
                    VALUES (%s,%s,%s,%s,%s,%s,%s)
                    ON CONFLICT (ref_key,kind,feature_version) DO UPDATE SET
                        status='pending',attempts=0,not_before=now(),error_code=NULL,updated_at=now()
                    WHERE (music_catalog_jobs.status='failed' AND music_catalog_jobs.updated_at<now()-interval '1 day')
                       OR (music_catalog_jobs.kind='expand' AND music_catalog_jobs.status='ready'
                           AND music_catalog_jobs.updated_at<now()-interval '7 days')
                    RETURNING id''',(ref.key,ref.contentType,ref.externalId,kind,MUSIC_FEATURE_VERSION,
                                    10 if kind=='prepare' else 0,[ref.key])).fetchone()
                if row:
                    count += 1
                    admitted += 1
        counts = dict(connection.execute('''SELECT status,count(*) FROM music_catalog_jobs
            WHERE feature_version=%s AND (ref_key=ANY(%s) OR parent_refs && %s::text[])
            GROUP BY status''',(MUSIC_FEATURE_VERSION,keys,keys)).fetchall())
        covered = connection.execute('''SELECT count(DISTINCT ref_key) FROM music_catalog_jobs
            WHERE ref_key=ANY(%s) AND feature_version=%s''',(keys,MUSIC_FEATURE_VERSION)).fetchone()[0]
    pending = sum(counts.get(status,0) for status in ['pending','running','retry'])
    failed = counts.get('failed',0)
    return {'status':'pending' if pending else 'failed' if failed else 'busy' if covered<len(keys) else 'ready',
            'pending_count':pending,'failed_count':failed,'retry_after_seconds':10}


class MusicCatalogWorker:
    def __init__(self, discovery=None):
        self.discovery = discovery
        self.stop_event = threading.Event()
        self.thread = None

    def start(self):
        if enabled():
            self.thread = threading.Thread(target=self.run,name='music-catalog',daemon=True)
            self.thread.start()
        return self

    def stop(self):
        self.stop_event.set()
        if self.thread:
            self.thread.join(timeout=1)

    def run(self):
        try:
            while not self.stop_event.is_set():
                try:
                    self.run_once()
                except Exception as error:
                    logger.warning('Music catalog temporarily unavailable (%s)',type(error).__name__)
                self.stop_event.wait(2)
        finally:
            if self.discovery:
                self.discovery.close()

    def run_once(self):
        with get_connection() as connection:
            with connection.cursor(row_factory=dict_row) as cursor:
                cursor.execute('SELECT pg_try_advisory_lock(%s) AS acquired', (WORKER_LOCK_ID,))
                if not cursor.fetchone()['acquired']:
                    return
                try:
                    cursor.execute('''UPDATE music_catalog_jobs SET status='running',locked_at=now(),updated_at=now()
                        WHERE id=(SELECT id FROM music_catalog_jobs WHERE feature_version=%s AND
                        ((status IN ('pending','retry') AND not_before<=now()) OR
                         (status='running' AND locked_at<now()-interval '5 minutes'))
                        ORDER BY priority DESC,id LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING *''',(MUSIC_FEATURE_VERSION,))
                    job = cursor.fetchone()
                    connection.commit()
                    if not job:
                        return
                    try:
                        self.process(job)
                    except PreparationDeferred as error:
                        failed_attempt = error.code in {'musicbrainz_limit','spotify_limit'}
                        terminal = failed_attempt and job['attempts']>=3
                        self.finish(job,'failed' if terminal else 'retry',error.code,error.seconds,failed_attempt)
                    except MappingUnavailable:
                        self.finish(job,'failed','mapping_unavailable',0,True)
                    except Exception as error:
                        status = getattr(getattr(error,'response',None),'status_code',None)
                        terminal = job['attempts']>=3 or status in {400,401,403,404,422}
                        self.finish(job,'failed' if terminal else 'retry',f'http_{status}' if status else type(error).__name__,
                                    min(3600,60*2**job['attempts']),True)
                    else:
                        self.finish(job,'ready')
                finally:
                    cursor.execute('SELECT pg_advisory_unlock(%s)', (WORKER_LOCK_ID,))

    @staticmethod
    def finish(job,status,code=None,seconds=0,failed_attempt=False):
        with get_connection() as connection:
            connection.execute('''UPDATE music_catalog_jobs SET status=%s,error_code=%s,
                attempts=attempts+%s,not_before=now()+(%s * interval '1 second'),locked_at=NULL,
                updated_at=now() WHERE id=%s''',(status,code,int(failed_attempt),seconds,job['id']))

    def resolver(self):
        if self.discovery is None:
            service_root = Path(__file__).resolve().parents[2]
            env_path = os.environ.get('MUSIC_CATALOG_SPOTIFY_ENV') or str(service_root.parent/'music-service'/'.env')
            # Optional local developer convenience; deployment can inject only
            # SPOTIFY_CLIENT_ID/SECRET directly. Credentials never enter DB/cache.
            if not Path(env_path).is_absolute():
                env_path = str(service_root/env_path)
            cache = service_root/'artifacts/musicbrainz-cache'
            self.discovery = MusicDiscovery(spotify_env=env_path,cache_dir=cache)
        return self.discovery

    def process(self,job):
        kind = job['content_type']
        saved = (get_music_features(kind,job['external_id']) if job['external_id'] else
                 get_music_by_mbid(kind,str(job['musicbrainz_id'])))
        current = bool(saved and saved['feature_version']==MUSIC_FEATURE_VERSION)
        if job['kind']=='prepare':
            if current:
                return
            resolver = self.resolver()
            if job['external_id']:
                mbid,evidence = resolver.resolve(MusicRef(contentType=kind,externalId=job['external_id']))
                if not mbid:
                    raise MappingUnavailable('no_unique_mapping')
                oid = job['external_id']
                existing = get_music_by_mbid(kind,mbid)
                if existing and existing['feature_version']==MUSIC_FEATURE_VERSION:
                    save_music_alias(kind,oid,existing['canonical_external_id'])
                    return
                original_oid = oid
                if existing:
                    oid = existing['canonical_external_id']
                features,metadata,source_hash = resolver.features(MusicRef(contentType=kind,externalId=oid),mbid)
            else:
                mbid = str(job['musicbrainz_id'])
                oid,features,metadata,source_hash,evidence = resolver.from_mbid(kind,mbid)
            save_music_features(kind,oid,mbid,features.model_dump(),metadata,source_hash)
            if job['external_id'] and job['external_id']!=oid:
                save_music_alias(kind,job['external_id'],oid)
            with get_connection() as connection:
                connection.execute('UPDATE music_catalog_jobs SET metadata=metadata || %s WHERE id=%s',
                    (Jsonb({'evidence':evidence,'canonicalExternalId':oid}),job['id']))
            return
        if not current:
            with get_connection() as connection:
                parent = connection.execute("SELECT status FROM music_catalog_jobs WHERE ref_key=%s AND kind='prepare' AND feature_version=%s",
                                            (job['ref_key'],MUSIC_FEATURE_VERSION)).fetchone()
            if parent and parent[0]=='failed':
                raise MappingUnavailable('reference_not_prepared')
            raise PreparationDeferred(30,'waiting_for_reference')
        known_artists = {artist for item in list_music_catalog() if item['contentType']=='ARTIST'
                         for artist in item['features'].get('artists',[])}
        candidates = self.resolver().discover(saved,known_artists=known_artists)
        with get_connection() as connection:
            connection.execute('SELECT pg_advisory_xact_lock(7512051)')
            count = connection.execute("SELECT count(*) FROM music_catalog_jobs WHERE status IN ('pending','running','retry')").fetchone()[0]
            for candidate_kind,mbid in candidates:
                existing = get_music_by_mbid(candidate_kind,mbid)
                if existing and existing['feature_version']==MUSIC_FEATURE_VERSION:
                    continue
                key = f'MB:{candidate_kind}:{mbid}'
                # Link a shared pending candidate to every source that needs it.
                previous = connection.execute("""SELECT id,status,updated_at<now()-interval '1 day'
                    FROM music_catalog_jobs WHERE ref_key=%s AND kind='prepare' AND feature_version=%s""",
                    (key,MUSIC_FEATURE_VERSION)).fetchone()
                if previous:
                    connection.execute("""UPDATE music_catalog_jobs SET parent_refs=
                        ARRAY(SELECT DISTINCT unnest(parent_refs || %s::text[])) WHERE id=%s""",
                        ([job['ref_key']],previous[0]))
                    if previous[1]=='failed' and previous[2] and count<200:
                        connection.execute("""UPDATE music_catalog_jobs SET status='pending',attempts=0,
                            error_code=NULL,not_before=now(),updated_at=now() WHERE id=%s""",(previous[0],))
                        count += 1
                    continue
                if count>=200:
                    raise PreparationDeferred(60,'queue_capacity')
                connection.execute('''INSERT INTO music_catalog_jobs
                    (ref_key,content_type,musicbrainz_id,kind,feature_version,priority,parent_refs)
                    VALUES (%s,%s,%s,'prepare',%s,-10,%s)''',
                    (key,candidate_kind,mbid,MUSIC_FEATURE_VERSION,[job['ref_key']]))
                count += 1
