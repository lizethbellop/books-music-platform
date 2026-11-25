"""Durable public catalog queue; credentials are never written to PostgreSQL."""
import logging
import os
import threading
import time
from datetime import datetime, timedelta, timezone

import httpx
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from ..book_features_repository import get_book_features
from ..books_client import get_book_metadata
from ..database import get_connection
from ..feature_extractor import EXTRACTOR_VERSION
from ..openlibrary import work_id
from ..provider_budget import PreparationDeferred, reserve_groq_call, provider_request, cooldown

logger = logging.getLogger(__name__)
WORKER_LOCK_ID = 7512042
# Short-lived tokens only help reuse Books metadata. After a restart the worker
# falls back to public OpenLibrary data without needing a user's credentials.
_tokens = {}
_token_lock = threading.Lock()



def enabled():
    return os.environ.get("BOOK_CATALOG_AUTOPREPARE", "true").lower() in {"true", "1", "yes"}


def version():
    return EXTRACTOR_VERSION, os.environ["GROQ_MODEL"].strip()



def enqueue_books(ids, known_ids=(), token=None):
    if not enabled():
        return {"status": "disabled", "pending_count": 0, "failed_count": 0}
    ids = sorted({oid for value in ids if (oid := work_id(value)) is not None})
    if not ids:
        return {"status": "ready", "pending_count": 0, "failed_count": 0}
    extractor, model = version()
    known_ids = set(known_ids)
    with get_connection() as connection:
        with connection.cursor() as cursor:
            # Serialize admission so parallel requests cannot overfill the queue.
            cursor.execute("SELECT pg_advisory_xact_lock(7512041)")
            cursor.execute("SELECT COUNT(*) FROM book_catalog_jobs WHERE status IN ('pending','retry','running')")
            available = max(0, 200 - cursor.fetchone()[0])
            cursor.execute('SELECT DISTINCT work_id FROM book_catalog_jobs WHERE work_id=ANY(%s) AND extractor_version=%s AND model_name=%s',
                           (ids, extractor, model))
            existing_ids = {row[0] for row in cursor.fetchall()}
            # Admit previously unseen references first on subsequent polls.
            for oid in sorted(ids, key=lambda value: (value in existing_ids, value))[:100]:
                for kind in (['expand'] if oid in known_ids else ['prepare', 'expand']):
                    if available <= 0:
                        break
                    cursor.execute("""INSERT INTO book_catalog_jobs(work_id,kind,extractor_version,model_name,priority)
                        VALUES (%s,%s,%s,%s,%s) ON CONFLICT (work_id,kind,extractor_version,model_name)
                        DO UPDATE SET status='pending', attempts=0, not_before=now(), error_code=NULL,
                            updated_at=now() WHERE book_catalog_jobs.status='failed'
                            AND book_catalog_jobs.updated_at < now()-interval '1 day' RETURNING id""",
                        (oid, kind, extractor, model, 10 if kind == 'prepare' else 0))
                    if cursor.fetchone():
                        available -= 1
            cursor.execute("""SELECT status, count(*) FROM book_catalog_jobs
                WHERE work_id=ANY(%s) AND extractor_version=%s AND model_name=%s
                GROUP BY status""", (ids, extractor, model))
            counts = dict(cursor.fetchall())
            cursor.execute("""SELECT child.status, count(*) FROM book_catalog_jobs child
                WHERE child.kind='prepare' AND NOT (child.work_id=ANY(%s))
                AND child.extractor_version=%s AND child.model_name=%s
                AND (child.metadata->>'requestedByBook'=ANY(%s) OR child.work_id IN (
                    SELECT jsonb_array_elements_text(parent.metadata->'candidateIds')
                    FROM book_catalog_jobs parent WHERE parent.work_id=ANY(%s)
                    AND parent.kind='expand' AND parent.extractor_version=%s AND parent.model_name=%s))
                GROUP BY child.status""",
                (ids, extractor, model, ids, ids, extractor, model))
            child_counts = dict(cursor.fetchall())
            children = sum(child_counts.get(k, 0) for k in ('pending', 'retry', 'running'))
    if token:
        with _token_lock:
            now = time.monotonic()
            for key in list(_tokens):
                if _tokens[key][1] < now:
                    del _tokens[key]
            for oid in ids[:100]:
                if oid not in known_ids and len(_tokens) < 200:
                    _tokens[oid] = (token, now + 120)
    pending = sum(counts.get(k, 0) for k in ('pending', 'retry', 'running')) + children
    failed = counts.get('failed', 0) + child_counts.get('failed', 0)
    # A saturated queue must never be reported as a completed preparation.
    missing = max(0, len(ids[:100]) - sum(counts.values()))
    return {"status": "pending" if pending else "failed" if failed else "busy" if missing else "ready",
            "pending_count": pending, "failed_count": failed, "retry_after_seconds": 10}


class BookCatalogWorker:
    def __init__(self):
        self.stop_event = threading.Event()
        self.thread = None
        self.last_public_call = 0

    def start(self):
        if enabled():
            self.thread = threading.Thread(target=self.run, name="book-catalog", daemon=True)
            self.thread.start()
        return self

    def stop(self):
        self.stop_event.set()
        # Provider timeouts are bounded; unfinished jobs are recovered by lease.
        if self.thread:
            self.thread.join(timeout=1)

    def run(self):
        while not self.stop_event.is_set():
            try:
                self.run_once()
            except Exception as error:
                # No URLs, response bodies, metadata, tokens or DB credentials.
                logger.warning("Catalog worker temporarily unavailable (%s)", type(error).__name__)
            self.stop_event.wait(2)

    def run_once(self):
        with _token_lock:
            for key in list(_tokens):
                if _tokens[key][1] < time.monotonic():
                    del _tokens[key]
        with get_connection() as connection:
            with connection.cursor(row_factory=dict_row) as cursor:
                # Session lock spans the job, including provider calls. Multiple
                # server processes still run only one catalog job at a time.
                cursor.execute("SELECT pg_try_advisory_lock(%s) AS acquired", (WORKER_LOCK_ID,))
                if not cursor.fetchone()['acquired']:
                    return
                extractor, model = version()
                cursor.execute("""UPDATE book_catalog_jobs SET status='running', locked_at=now(), updated_at=now()
                    WHERE id=(SELECT id FROM book_catalog_jobs
                        WHERE extractor_version=%s AND model_name=%s AND
                        ((status IN ('pending','retry') AND not_before<=now()) OR
                         (status='running' AND locked_at < now()-interval '5 minutes'))
                        ORDER BY priority DESC, id LIMIT 1 FOR UPDATE SKIP LOCKED)
                    RETURNING *""", (extractor, model))
                job = cursor.fetchone()
                connection.commit()
                if not job:
                    return
                try:
                    self.process(job)
                except PreparationDeferred as error:
                    provider_failure = error.code in {'groq_limit', 'openlibrary_limit'}
                    terminal = provider_failure and job['attempts'] >= 3
                    self.finish(job['id'], 'failed' if terminal else 'retry', error.code, error.seconds,
                                failed_attempt=provider_failure)
                except Exception as error:
                    status = getattr(getattr(error, 'response', None), 'status_code', None)
                    terminal = status in (400, 404, 422) or job['attempts'] >= 3
                    self.finish(job['id'], 'failed' if terminal else 'retry',
                                f'http_{status}' if status else type(error).__name__,
                                min(3600, 60 * 2 ** job['attempts']))
                else:
                    self.finish(job['id'], 'ready')
                finally:
                    # Also release on connection pools, not just socket close.
                    cursor.execute("SELECT pg_advisory_unlock(%s)", (WORKER_LOCK_ID,))

    @staticmethod
    def finish(job_id, status, code=None, seconds=0, failed_attempt=True):
        with get_connection() as connection:
            connection.execute("""UPDATE book_catalog_jobs SET status=%s, error_code=%s,
                attempts=attempts+%s, not_before=now()+(%s * interval '1 second'),
                locked_at=NULL, updated_at=now() WHERE id=%s""",
                (status, code, int(failed_attempt and status != 'ready'), seconds, job_id))

    def public_get(self, path, params=None):
        remaining = 1.1 - (time.monotonic() - self.last_public_call)
        if remaining > 0 and self.stop_event.wait(remaining):
            raise PreparationDeferred(5, 'shutdown')
        self.last_public_call = time.monotonic()
        with httpx.Client(timeout=15, follow_redirects=True, headers={
            'User-Agent': 'musa-recommendations/0.1 (https://github.com/lizethbellop/books-music-platform)'
        }) as client:
            response = provider_request(client, 'openlibrary', 'GET', 'https://openlibrary.org' + path, params=params)
            if response.status_code == 429:
                raise PreparationDeferred(120, 'openlibrary_limit')
            response.raise_for_status()
            return response.json()

    def source_book(self, oid, hints=None):
        hints = hints or {}
        data = self.public_get(f'/works/{oid}.json')
        authors = hints.get('authors', [])
        if not authors:
            import re
            for entry in data.get('authors', [])[:2]:
                key = entry.get('author', {}).get('key', '')
                if re.fullmatch(r'/authors/OL[0-9]+A', key):
                    name = self.public_get(key + '.json').get('name')
                    if isinstance(name, str):
                        authors.append(name)
        description = data.get('description', '')
        if isinstance(description, dict):
            description = description.get('value', '')
        covers = [v for v in data.get('covers', []) if isinstance(v, int) and v > 0]
        return {'externalId': oid, 'title': data['title'], 'authors': authors,
                'subjects': data.get('subjects', []), 'description': description,
                'coverUrl': f'https://covers.openlibrary.org/b/id/{covers[0]}-M.jpg' if covers else hints.get('coverUrl')}

    def process(self, job):
        oid = job['work_id']
        saved = get_book_features(oid)
        current = bool(saved and saved.get('metadata', {}).get('title') and saved['extractor_version'] == job['extractor_version']
                       and saved['model_name'] == job['model_name'])
        if job['kind'] == 'expand':
            if not current:
                with get_connection() as connection:
                    row = connection.execute("""SELECT status FROM book_catalog_jobs WHERE work_id=%s
                        AND kind='prepare' AND extractor_version=%s AND model_name=%s""",
                        (oid, job['extractor_version'], job['model_name'])).fetchone()
                if row and row[0] == 'failed':
                    raise ValueError('reference_unavailable')
                raise PreparationDeferred(30, 'waiting_for_reference')
            self.expand(oid, saved, job['id'])
            return
        if current:
            return
        book = job['metadata'].get('book')
        if not book:
            with _token_lock:
                entry = _tokens.pop(oid, None)
            if entry and entry[1] > time.monotonic():
                try:
                    book = get_book_metadata(oid, entry[0])
                except (httpx.HTTPError, ValueError):
                    book = None
            if not book:
                book = self.source_book(oid, job['metadata'].get('hints'))
            # Whitelist public fields; never persist arbitrary upstream payloads.
            book = {key: book.get(key) for key in (
                'externalId', 'title', 'authors', 'subjects', 'description', 'coverUrl', 'firstPublishDate')}
            with get_connection() as connection:
                connection.execute("UPDATE book_catalog_jobs SET metadata=metadata || %s WHERE id=%s",
                                   (Jsonb({'book': book}), job['id']))
        if not os.environ.get('GROQ_API_KEY', '').strip():
            raise PreparationDeferred(600, 'provider_not_configured')
        from .book_features_service import get_or_extract_book_features
        try:
            get_or_extract_book_features(book)
        except httpx.HTTPStatusError as error:
            if error.response.status_code == 429:
                delay = error.response.headers.get('Retry-After', '')
                seconds = min(86400, max(35, int(delay))) if delay.isdigit() else 120
                cooldown('groq', seconds)
                raise PreparationDeferred(seconds, 'groq_limit') from None
            raise

    def expand(self, oid, saved, job_id):
        from .book_knn import feature_label
        labels = []
        for group in ('themes', 'genres'):
            for value in saved['features'].get(group, []):
                label = feature_label(group, value)
                if label and label not in {'fiction', 'adult', 'novel'} and label not in labels:
                    labels.append(label)
                if len(labels) >= 3:
                    break
            if len(labels) >= 3:
                break
        if not labels:
            return
        # Provider search selects candidates only. KNN still decides similarity.
        query = ' OR '.join('subject:"' + label.replace('"', '')[:100] + '"' for label in labels)
        result = self.public_get('/search.json', {'q': query, 'limit': 20,
            'fields': 'key,title,author_name,subject,cover_i'})
        maximum = min(8, max(1, int(os.environ.get('BOOK_CATALOG_CANDIDATES_PER_BOOK', '3'))))
        candidates = []
        for row in result.get('docs', []):
            candidate = work_id(row.get('key', ''))
            if not candidate or candidate == oid or candidate in {item[0] for item in candidates}:
                continue
            existing = get_book_features(candidate)
            if existing and existing['extractor_version'] == EXTRACTOR_VERSION and existing['model_name'] == version()[1]:
                continue
            cover = row.get('cover_i')
            candidates.append((candidate, {'authors': row.get('author_name', []),
                'coverUrl': f'https://covers.openlibrary.org/b/id/{cover}-M.jpg' if isinstance(cover, int) and cover > 0 else None}))
            if len(candidates) >= maximum:
                break
        with get_connection() as connection:
            connection.execute('SELECT pg_advisory_xact_lock(7512041)')
            count = connection.execute("SELECT count(*) FROM book_catalog_jobs WHERE status IN ('pending','running','retry')").fetchone()[0]
            admitted = candidates[:max(0, 200-count)]
            connection.execute('UPDATE book_catalog_jobs SET metadata=metadata || %s WHERE id=%s',
                               (Jsonb({'candidateIds': [item[0] for item in admitted]}), job_id))
            for candidate, hints in admitted:
                connection.execute("""INSERT INTO book_catalog_jobs
                    (work_id,kind,extractor_version,model_name,metadata,priority)
                    VALUES (%s,'prepare',%s,%s,%s,-10) ON CONFLICT DO NOTHING""",
                    (candidate, *version(), Jsonb({'requestedByBook': oid, 'hints': hints})))
        if len(admitted) < len(candidates):
            raise PreparationDeferred(60, 'queue_capacity')
