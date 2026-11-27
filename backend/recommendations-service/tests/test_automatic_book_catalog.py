"""Real PostgreSQL queue/cache checks, with all external providers replaced.
Run explicitly with RUN_CATALOG_DB_TESTS=1; creates and drops an isolated schema.
"""
import os
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4

import httpx
import psycopg
from psycopg import sql
import pytest

from recommender import book_features_repository as repository
from recommender import provider_budget, music_features_repository
from recommender.service import music_catalog_jobs
from recommender.feature_extractor import BookFeatures
from recommender.service import book_catalog_jobs as jobs
from recommender.service import book_features_service as extraction
from recommender.service.book_recommendations_service import recommend_similar_books

pytestmark = pytest.mark.skipif(os.environ.get('RUN_CATALOG_DB_TESTS') != '1', reason='Opt-in isolated PostgreSQL tests')


@pytest.fixture
def catalog_db(monkeypatch):
    real_connect = jobs.get_connection
    schema = 'catalog_test_' + uuid4().hex
    with real_connect() as connection:
        connection.execute(sql.SQL('CREATE SCHEMA {}').format(sql.Identifier(schema)))
    def connect():
        connection = real_connect()
        connection.execute(sql.SQL('SET search_path TO {}').format(sql.Identifier(schema)))
        connection.commit()
        return connection
    monkeypatch.setattr(jobs, 'get_connection', connect)
    # Advisory locks are DB-wide, not schema-local. Avoid competing with the
    # user's running service while exercising isolated test tables.
    monkeypatch.setattr(jobs, 'WORKER_LOCK_ID', uuid4().int % 1000000000 + 10000000)
    monkeypatch.setattr(music_catalog_jobs, 'WORKER_LOCK_ID', -(uuid4().int % 1000000000 + 10000000))
    monkeypatch.setattr(provider_budget, 'get_connection', connect)
    monkeypatch.setattr(music_features_repository, 'get_connection', connect)
    monkeypatch.setattr(music_catalog_jobs, 'get_connection', connect)
    monkeypatch.setenv('MUSIC_CATALOG_AUTOPREPARE', 'true')
    monkeypatch.setattr(repository, 'get_connection', connect)
    monkeypatch.setattr(extraction, 'get_connection', connect)
    monkeypatch.setenv('BOOK_CATALOG_AUTOPREPARE', 'true')
    monkeypatch.setenv('GROQ_MODEL', 'catalog-test-model')
    monkeypatch.setenv('GROQ_API_KEY', 'test-placeholder-never-sent')
    monkeypatch.setenv('BOOK_CATALOG_GROQ_DAILY_LIMIT', '40')
    monkeypatch.setenv('BOOK_CATALOG_GROQ_INTERVAL_SECONDS', '1')
    jobs._tokens.clear()
    try:
        with connect() as connection:
            setup = Path(__file__).resolve().parents[1] / 'database/setup.sql'
            ddl = setup.read_text().split('BEGIN;', 1)[1].split('COMMIT;', 1)[0]
            connection.execute(ddl)
            migration = Path(__file__).resolve().parents[1] / 'database/004_automatic_book_catalog.sql'
            connection.execute(migration.read_text())
            connection.execute(migration.read_text())
        yield connect
    finally:
        jobs._tokens.clear()
        with real_connect() as connection:
            connection.execute(sql.SQL('DROP SCHEMA {} CASCADE').format(sql.Identifier(schema)))


def source(oid):
    return {'externalId': oid, 'title': oid, 'authors': ['Test author'],
            'subjects': ['relationships'], 'description': '', 'coverUrl': None}


def fake_extractor(monkeypatch):
    calls = []
    def extract(metadata):
        jobs.reserve_groq_call()
        calls.append(metadata['title'])
        return BookFeatures(genres=['romance'], themes=['relationships'], settings=[], tropes=[], audience=[], formats=[])
    monkeypatch.setattr(extraction, 'extract_book_features', extract)
    return calls


def allow_next(connect):
    with connect() as connection:
        connection.execute("UPDATE book_provider_budget SET next_call_at=now()-interval '1 second'")
        connection.execute("UPDATE book_catalog_jobs SET not_before=now() WHERE status='retry'")


def test_request_prepares_and_expands_then_reuses_shared_cache(catalog_db, monkeypatch):
    calls = fake_extractor(monkeypatch)
    books_calls = []
    monkeypatch.setattr(jobs, 'get_book_metadata', lambda oid, token: books_calls.append((oid, token)) or source(oid))
    monkeypatch.setattr(jobs.BookCatalogWorker, 'source_book', lambda self, oid, hints=None: source(oid))
    monkeypatch.setattr(jobs.BookCatalogWorker, 'public_get', lambda *args, **kwargs: {
        'docs': [{'key': '/works/OL2W'}, {'key': '/works/OL2W'}, {'key': '/works/OL1W'}]})
    result = recommend_similar_books('OL1W', token='temporary-session')
    assert result['preparation']['status'] == 'pending'
    assert result['items'] == []
    # A different process can pick up the persisted jobs.
    jobs.BookCatalogWorker().run_once()  # reference, using Books metadata
    jobs.BookCatalogWorker().run_once()  # related candidate discovery
    assert recommend_similar_books('OL1W')['preparation']['status'] == 'pending'
    allow_next(catalog_db)
    jobs.BookCatalogWorker().run_once()  # candidate, no recursive expansion
    result = recommend_similar_books('OL1W')
    assert [item['externalId'] for item in result['items']] == ['OL2W']
    assert result['preparation']['status'] == 'ready'
    assert calls == ['OL1W', 'OL2W']
    assert books_calls == [('OL1W', 'temporary-session')]
    extraction.get_or_extract_book_features(source('OL1W'))
    assert len(calls) == 2
    with catalog_db() as connection:
        assert connection.execute('SELECT count(*) FROM book_catalog_jobs').fetchone()[0] == 3
        rows = connection.execute('SELECT metadata::text FROM book_catalog_jobs').fetchall()
        assert all('temporary-session' not in row[0] for row in rows)


def test_daily_budget_is_shared_and_resets_without_provider_calls(catalog_db, monkeypatch):
    monkeypatch.setenv('BOOK_CATALOG_GROQ_DAILY_LIMIT', '1')
    jobs.reserve_groq_call()
    with pytest.raises(jobs.PreparationDeferred) as limited:
        jobs.reserve_groq_call()
    assert limited.value.code == 'daily_limit'
    with catalog_db() as connection:
        assert connection.execute("SELECT calls FROM book_provider_budget WHERE provider='groq'").fetchone()[0] == 1
        connection.execute("UPDATE book_provider_budget SET budget_day=budget_day-1, next_call_at=now()-interval '1 second'")
    jobs.reserve_groq_call()
    with catalog_db() as connection:
        assert connection.execute("SELECT calls FROM book_provider_budget WHERE provider='groq'").fetchone()[0] == 1


def test_restart_recovers_abandoned_job_without_token(catalog_db, monkeypatch):
    calls = fake_extractor(monkeypatch)
    monkeypatch.setattr(jobs.BookCatalogWorker, 'source_book', lambda self, oid, hints=None: source(oid))
    jobs.enqueue_books(['OL1W'], token='expired-not-persisted')
    jobs._tokens.clear()
    with catalog_db() as connection:
        connection.execute("UPDATE book_catalog_jobs SET status='running',locked_at=now()-interval '10 minutes' WHERE kind='prepare'")
    jobs.BookCatalogWorker().run_once()
    assert calls == ['OL1W']
    assert repository.get_book_features('OL1W')['metadata']['title'] == 'OL1W'


def test_concurrent_extraction_uses_single_provider_call(catalog_db, monkeypatch):
    calls = fake_extractor(monkeypatch)
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(extraction.get_or_extract_book_features, [source('OL1W'), source('OL1W')]))
    assert calls == ['OL1W']
    assert results[0] == results[1]


def test_provider_limit_defers_and_preserves_public_metadata(catalog_db, monkeypatch):
    monkeypatch.setattr(jobs.BookCatalogWorker, 'source_book', lambda self, oid, hints=None: source(oid))
    def limited(metadata):
        jobs.reserve_groq_call()
        response = httpx.Response(429, headers={'Retry-After': '60'}, request=httpx.Request('POST', 'https://provider.invalid'))
        response.raise_for_status()
    monkeypatch.setattr(extraction, 'extract_book_features', limited)
    jobs.enqueue_books(['OL1W'])
    jobs.BookCatalogWorker().run_once()
    with catalog_db() as connection:
        row = connection.execute("SELECT status,error_code,attempts,metadata FROM book_catalog_jobs WHERE kind='prepare'").fetchone()
        assert row[:3] == ('retry', 'groq_limit', 1)
        assert row[3]['book']['externalId'] == 'OL1W'


def test_queue_capacity_is_bounded(catalog_db):
    for start in range(0, 300, 100):
        jobs.enqueue_books([f'OL{i+1}W' for i in range(start, start+100)])
    with catalog_db() as connection:
        assert connection.execute('SELECT count(*) FROM book_catalog_jobs').fetchone()[0] == 200
    assert jobs.enqueue_books(['OL999W'])['status'] == 'busy'


def test_disabled_provider_budget_never_sends_http(catalog_db, monkeypatch):
    from recommender.provider_budget import provider_request
    monkeypatch.setenv('OPENLIBRARY_DAILY_LIMIT', '0')
    def unexpected(request):
        pytest.fail('Exhausted quota must not reach the external API')
    with httpx.Client(transport=httpx.MockTransport(unexpected)) as client:
        with pytest.raises(jobs.PreparationDeferred) as error:
            provider_request(client, 'openlibrary', 'GET', 'https://openlibrary.org/search.json')
    assert error.value.code == 'openlibrary_daily_limit'


def test_provider_budgets_are_independent_and_persistent(catalog_db, monkeypatch):
    from recommender.provider_budget import reserve_provider_call
    monkeypatch.setenv('OPENLIBRARY_DAILY_LIMIT', '1')
    monkeypatch.setenv('SPOTIFY_DAILY_LIMIT', '1')
    reserve_provider_call('openlibrary')
    reserve_provider_call('spotify')
    for provider in ('openlibrary', 'spotify'):
        with pytest.raises(jobs.PreparationDeferred):
            reserve_provider_call(provider)
    with catalog_db() as connection:
        counts = dict(connection.execute('SELECT provider,calls FROM book_provider_budget').fetchall())
    assert counts['openlibrary'] == counts['spotify'] == 1
    assert counts['groq'] == 0


def test_429_has_a_finite_attempt_limit(catalog_db, monkeypatch):
    monkeypatch.setattr(jobs.BookCatalogWorker, 'source_book', lambda self, oid, hints=None: source(oid))
    calls = []
    def limited(metadata):
        jobs.reserve_groq_call()
        calls.append(1)
        httpx.Response(429, request=httpx.Request('POST', 'https://provider.invalid')).raise_for_status()
    monkeypatch.setattr(extraction, 'extract_book_features', limited)
    jobs.enqueue_books(['OL1W'])
    for _ in range(4):
        allow_next(catalog_db)
        jobs.BookCatalogWorker().run_once()
    with catalog_db() as connection:
        assert connection.execute("SELECT status,attempts FROM book_catalog_jobs WHERE kind='prepare'").fetchone() == ('failed',4)
    assert len(calls) == 4


@pytest.mark.parametrize('package,provider,migration', [
    ('books','openlibrary','001_provider_budget.sql'), ('music','spotify','002_provider_budget.sql')])
def test_java_reservation_sql_limits_and_reset(catalog_db, package, provider, migration):
    backend = Path(__file__).resolve().parents[2]
    service = backend / (package+'-service')
    ddl = (service/'database/migrations'/migration).read_text()
    java = (service/f'src/main/java/com/musa/{package}/budget/ProviderBudget.java').read_text()
    query = java.split('RESERVE_SQL = """',1)[1].split('"""',1)[0].replace('?', '%s')
    with catalog_db() as connection:
        connection.execute(ddl)
        connection.execute(ddl)
        assert connection.execute(query,(provider,2,10)).fetchone() == (1,)
        assert connection.execute(query,(provider,2,10)).fetchone() == (2,)
        assert connection.execute(query,(provider,2,10)).fetchone() is None
        connection.execute("UPDATE external_api_budget SET budget_day=budget_day-1,window_start=now()-interval '2 minutes'")
        assert connection.execute(query,(provider,2,10)).fetchone() == (1,)
        connection.execute("UPDATE external_api_budget SET blocked_until=now()+interval '1 hour'")
        assert connection.execute(query,(provider,2,10)).fetchone() is None


@pytest.mark.parametrize('package,provider,migration', [
    ('books','openlibrary','001_provider_budget.sql'), ('music','spotify','002_provider_budget.sql')])
def test_java_sql_is_atomic_across_concurrent_requests(catalog_db, package, provider, migration):
    service = Path(__file__).resolve().parents[2] / (package+'-service')
    java = (service/f'src/main/java/com/musa/{package}/budget/ProviderBudget.java').read_text()
    query = java.split('RESERVE_SQL = """',1)[1].split('"""',1)[0].replace('?', '%s')
    with catalog_db() as connection:
        connection.execute((service/'database/migrations'/migration).read_text())
    def attempt(_):
        with catalog_db() as connection:
            return connection.execute(query,(provider,3,3)).fetchone() is not None
    with ThreadPoolExecutor(max_workers=6) as pool:
        results = list(pool.map(attempt,range(6)))
    assert sum(results) == 3
    with catalog_db() as connection:
        assert connection.execute('SELECT calls,minute_calls FROM external_api_budget').fetchone() == (3,3)


def test_external_429_cooldown_blocks_further_http(catalog_db):
    from recommender.provider_budget import provider_request
    calls = []
    def handler(request):
        calls.append(1)
        return httpx.Response(429, headers={'Retry-After': '90'})
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(jobs.PreparationDeferred):
            provider_request(client, 'openlibrary', 'GET', 'https://openlibrary.org/search.json')
        with pytest.raises(jobs.PreparationDeferred):
            provider_request(client, 'openlibrary', 'GET', 'https://openlibrary.org/search.json')
    assert calls == [1]


def test_direct_groq_extractor_obeys_zero_budget(catalog_db, monkeypatch):
    from recommender.feature_extractor import extract_book_features
    monkeypatch.setenv('BOOK_CATALOG_GROQ_DAILY_LIMIT', '0')
    monkeypatch.setattr(httpx, 'Client', lambda **kwargs: pytest.fail('No HTTP client should be created'))
    with pytest.raises(jobs.PreparationDeferred):
        extract_book_features({'title': 'Example', 'subjects': []})


def _music_seed(oid='lush'):
    from uuid import UUID
    from recommender.music_features_repository import save_music_features
    artist='134e6410-6954-45d1-bd4a-0f2d2ad5471d'
    mbid='00000000-0000-0000-0000-000000000001'
    save_music_features('SONG',oid,mbid,{'genres':['synth-pop'],'tags':[],'artists':[artist]},
        {'name':'Lush Life','artistNames':['Zara Larsson'],'sourceUrl':'https://musicbrainz.org/recording/'+mbid},'a'*64)
    return artist,mbid


class FakeMusicDiscovery:
    artist='134e6410-6954-45d1-bd4a-0f2d2ad5471d'
    album='00000000-0000-0000-0000-000000000002'
    song='00000000-0000-0000-0000-000000000003'
    def discover(self,saved,known_artists=()):
        return [('ARTIST',self.artist),('ALBUM',self.album),('SONG',self.song)]
    def from_mbid(self,kind,mbid):
        from recommender.music_models import MusicFeatures
        return (kind.lower()+'-new',MusicFeatures(genres=['synth-pop'],tags=[],artists=[self.artist]),
            {'name':kind+' discovery','artistNames':['Zara Larsson'],'sourceUrl':'https://musicbrainz.org/artist/'+mbid},'b'*64,'test-verified')
    def resolve(self,ref):
        return '00000000-0000-0000-0000-000000000001','test-verified'


def test_music_catalog_expands_automatically_and_reuses_tasks(catalog_db,monkeypatch):
    from recommender.service.music_catalog_jobs import enqueue_music,MusicCatalogWorker
    from recommender.music_features_repository import list_music_catalog
    from recommender.service.music_knn import recommend_music
    from recommender.service.music_tastes import MusicTastes
    _music_seed()
    worker=MusicCatalogWorker(FakeMusicDiscovery())
    for _ in range(3):
        assert enqueue_music(['SONG:lush'])['status']=='pending'
    worker.run_once()  # expand positive seed
    assert enqueue_music(['SONG:lush'])['status']=='pending'
    for _ in range(3): worker.run_once()  # prepare artist, album, song
    assert enqueue_music(['SONG:lush'])['status']=='ready'
    catalog=list_music_catalog()
    for kind in ['ARTIST','ALBUM','SONG']:
        result=recommend_music(catalog,MusicTastes({'SONG:lush':3},{'SONG:lush'}),kind)
        assert result['items'][0]['artistNames']==['Zara Larsson']
    with catalog_db() as connection:
        assert connection.execute('SELECT count(*) FROM music_catalog_jobs').fetchone()[0]==4
        assert connection.execute("SELECT count(*) FROM music_catalog_jobs WHERE kind='expand'").fetchone()[0]==1
    # Stored sources mean future visits need no provider calls.
    monkeypatch.setattr(FakeMusicDiscovery,'discover',lambda *args:pytest.fail('Must reuse completed job'))
    worker.run_once()


def test_alternate_spotify_ids_reuse_same_recording(catalog_db,monkeypatch):
    from recommender.service.music_catalog_jobs import enqueue_music,MusicCatalogWorker
    from recommender.music_features_repository import get_music_features,canonical_music_keys
    _music_seed()
    enqueue_music(['SONG:alternate'])
    MusicCatalogWorker(FakeMusicDiscovery()).run_once()
    assert get_music_features('SONG','alternate')['canonical_external_id']=='lush'
    assert canonical_music_keys(['SONG:alternate'])=={'SONG:alternate':'SONG:lush'}
    with catalog_db() as connection:
        assert connection.execute('SELECT count(*) FROM music_features').fetchone()[0]==1


def test_music_429_is_bounded_and_restart_recovers_work(catalog_db):
    from recommender.service.music_catalog_jobs import enqueue_music,MusicCatalogWorker
    _music_seed()
    enqueue_music(['SONG:lush'])
    with catalog_db() as connection:
        connection.execute("UPDATE music_catalog_jobs SET status='running',locked_at=now()-interval '10 minutes'")
    class Limited(FakeMusicDiscovery):
        def discover(self,saved,known_artists=()):
            raise jobs.PreparationDeferred(90,'musicbrainz_limit')
    for _ in range(4):
        with catalog_db() as connection:
            connection.execute('UPDATE music_catalog_jobs SET not_before=now()')
        MusicCatalogWorker(Limited()).run_once()
    with catalog_db() as connection:
        assert connection.execute('SELECT status,attempts FROM music_catalog_jobs').fetchone()==('failed',4)


def test_music_queue_admission_is_bounded(catalog_db):
    from recommender.service.music_catalog_jobs import enqueue_music
    for start in range(0,300,100):
        enqueue_music([f'SONG:source{i}' for i in range(start,start+100)])
    with catalog_db() as connection:
        assert connection.execute('SELECT count(*) FROM music_catalog_jobs').fetchone()[0]==200
    assert enqueue_music(['SONG:overflow'])['status']=='busy'


def test_personalized_music_request_enqueues_and_handles_alias_exclusions(catalog_db,monkeypatch):
    from recommender.service import music_recommendations_service as service
    from recommender.service.music_catalog_jobs import MusicCatalogWorker,enqueue_music
    from recommender.service.music_tastes import MusicTastes
    _music_seed()
    enqueue_music(['SONG:alternate'])
    worker=MusicCatalogWorker(FakeMusicDiscovery())
    worker.run_once()  # alternate ID maps to existing Lush Life
    monkeypatch.setattr(service,'get_music_tastes',lambda token:MusicTastes({'SONG:alternate':3},{'SONG:alternate'}))
    result=service.recommend_music_for_user('temporary-session','SONG')
    assert result['preparation']['status']=='pending'
    assert all(item['spotifyId']!='lush' for item in result['items'])
    for _ in range(5): worker.run_once()
    result=service.recommend_music_for_user('temporary-session','SONG')
    assert [item['spotifyId'] for item in result['items']]==['song-new']
    assert result['preparation']['status']=='ready'
