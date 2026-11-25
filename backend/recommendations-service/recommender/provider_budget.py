"""Persistent quotas for external requests issued by recommendations-service."""
import os
import time
from datetime import datetime, timedelta, timezone
from urllib.parse import urljoin, urlparse

from .database import get_connection


class PreparationDeferred(ValueError):
    def __init__(self, seconds, code='rate_limit'):
        self.seconds = max(1, int(seconds))
        self.code = code
        super().__init__(code)


DEFAULTS = {'groq': (40, 35), 'openlibrary': (150, 3), 'spotify': (20, 2), 'musicbrainz': (150, 2)}


def reserve_provider_call(provider):
    daily_default, interval_default = DEFAULTS[provider]
    prefix = 'BOOK_CATALOG_GROQ' if provider == 'groq' else provider.upper()
    daily = max(0, int(os.environ.get(prefix + '_DAILY_LIMIT', str(daily_default))))
    interval = max(1, float(os.environ.get(prefix + '_INTERVAL_SECONDS', str(interval_default))))
    now = datetime.now(timezone.utc)
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute('''INSERT INTO book_provider_budget(provider,budget_day,calls,next_call_at)
                VALUES (%s,%s,0,%s) ON CONFLICT DO NOTHING''', (provider, now.date(), now))
            cursor.execute('SELECT budget_day,calls,next_call_at FROM book_provider_budget WHERE provider=%s FOR UPDATE', (provider,))
            day, calls, next_at = cursor.fetchone()
            if day != now.date():
                calls = 0
            if calls >= daily:
                tomorrow = datetime.combine(now.date() + timedelta(days=1), datetime.min.time(), timezone.utc)
                raise PreparationDeferred((tomorrow-now).total_seconds()+1, provider+'_daily_limit')
            if next_at > now:
                raise PreparationDeferred((next_at-now).total_seconds()+1, provider+'_rate_limit')
            cursor.execute('''UPDATE book_provider_budget SET budget_day=%s,calls=%s,next_call_at=%s
                WHERE provider=%s''', (now.date(), calls+1, now+timedelta(seconds=interval), provider))


def reserve_groq_call():
    try:
        reserve_provider_call('groq')
    except PreparationDeferred as error:
        # Preserve the existing queue/API codes for Groq.
        if error.code == 'groq_daily_limit':
            error.code = 'daily_limit'
        raise


def cooldown(provider, seconds):
    seconds = min(86400, max(1, int(seconds)))
    with get_connection() as connection:
        connection.execute('''UPDATE book_provider_budget SET
            next_call_at=GREATEST(next_call_at,now()+(%s * interval '1 second')) WHERE provider=%s''',
            (seconds, provider))


def provider_request(client, provider, method, url, **kwargs):
    allowed = {'openlibrary': {'openlibrary.org'}, 'spotify': {'api.spotify.com','accounts.spotify.com'}, 'musicbrainz': {'musicbrainz.org'}}[provider]
    for hop in range(3):
        if urlparse(url).scheme != 'https' or urlparse(url).hostname not in allowed:
            raise ValueError('Unexpected provider URL')
        try:
            reserve_provider_call(provider)
        except PreparationDeferred as error:
            # Only a brief spacing delay is waited here; daily quotas and long
            # provider cooldowns are returned to the durable queue/caller.
            if error.code == provider+'_rate_limit' and error.seconds <= 5:
                time.sleep(error.seconds)
                reserve_provider_call(provider)
            else:
                raise
        response = client.request(method, url, follow_redirects=False, **kwargs)
        if response.status_code == 429:
            value = response.headers.get('Retry-After', '')
            delay = min(86400, max(60, int(value))) if value.isdigit() else 120
            cooldown(provider, delay)
            raise PreparationDeferred(delay, provider+'_limit')
        if response.status_code in (301,302,303,307,308) and method == 'GET':
            url = urljoin(url, response.headers.get('Location', ''))
            kwargs.pop('params', None)
            continue
        response.raise_for_status()
        return response
    raise ValueError('Too many provider redirects')
