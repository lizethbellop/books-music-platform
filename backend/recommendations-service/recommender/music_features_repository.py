from psycopg.types.json import Jsonb

from .database import get_connection
from .music_models import MUSIC_FEATURE_VERSION


def _music_record(row):
    return None if row is None else dict(zip(
        ('musicbrainz_id','feature_version','canonical_external_id','features','metadata'),row))


def get_music_features(content_type: str, external_id: str):
    with get_connection() as connection:
        row = connection.execute("""SELECT f.musicbrainz_id::text,f.feature_version,f.external_id,f.features,f.metadata
            FROM music_features f WHERE f.content_type=%s AND
            (f.external_id=%s OR f.external_id IN (SELECT canonical_external_id FROM music_feature_aliases
              WHERE content_type=%s AND external_id=%s))
            ORDER BY (f.external_id=%s) DESC LIMIT 1""",
            (content_type,external_id,content_type,external_id,external_id)).fetchone()
    return _music_record(row)


def get_music_by_mbid(content_type, mbid):
    with get_connection() as connection:
        row=connection.execute("""SELECT musicbrainz_id::text,feature_version,external_id,features,metadata
            FROM music_features WHERE content_type=%s AND musicbrainz_id=%s""",(content_type,mbid)).fetchone()
    return _music_record(row)


def save_music_alias(content_type, external_id, canonical_external_id):
    if external_id==canonical_external_id:
        return
    with get_connection() as connection:
        connection.execute("""INSERT INTO music_feature_aliases(content_type,external_id,canonical_external_id)
            VALUES (%s,%s,%s) ON CONFLICT (content_type,external_id)
            DO UPDATE SET canonical_external_id=EXCLUDED.canonical_external_id""",
            (content_type,external_id,canonical_external_id))


def canonical_music_keys(keys):
    with get_connection() as connection:
        rows=connection.execute("""SELECT content_type || ':' || external_id,
            content_type || ':' || canonical_external_id FROM music_feature_aliases
            WHERE content_type || ':' || external_id = ANY(%s)""",(list(keys),)).fetchall()
    return dict(rows)


def save_music_features(content_type, external_id, musicbrainz_id, features, metadata, source_hash):
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO music_features (
                    content_type, external_id, musicbrainz_id, features, metadata,
                    source_hash, source_name, feature_version, mapping_verified
                ) VALUES (%s, %s, %s, %s, %s, %s, 'musicbrainz', %s, true)
                ON CONFLICT (content_type, external_id) DO UPDATE SET
                    musicbrainz_id = EXCLUDED.musicbrainz_id,
                    features = EXCLUDED.features, metadata = EXCLUDED.metadata,
                    source_hash = EXCLUDED.source_hash,
                    source_name = EXCLUDED.source_name,
                    feature_version = EXCLUDED.feature_version,
                    mapping_verified = EXCLUDED.mapping_verified,
                    updated_at = CURRENT_TIMESTAMP
                """,
                (content_type, external_id, musicbrainz_id, Jsonb(features),
                 Jsonb(metadata), source_hash, MUSIC_FEATURE_VERSION),
            )


def list_music_catalog() -> list[dict]:
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """SELECT content_type, external_id, metadata, features
                   FROM music_features
                   WHERE feature_version = %s AND source_name = 'musicbrainz'
                     AND mapping_verified = true
                   ORDER BY content_type, external_id""",
                (MUSIC_FEATURE_VERSION,),
            )
            rows = cursor.fetchall()
    return [
        {"key": f"{kind}:{oid}", "contentType": kind, "externalId": oid,
         "metadata": metadata, "features": features}
        for kind, oid, metadata, features in rows
    ]
