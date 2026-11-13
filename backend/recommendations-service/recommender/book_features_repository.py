from psycopg.types.json import Jsonb

from .database import get_connection


def get_book_features(work_id: str):
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT features, source_hash, extractor_version, model_name, metadata
                FROM book_features WHERE work_id = %s
                """,
                (work_id,),
            )
            row = cursor.fetchone()
    if row is None:
        return None
    return dict(zip(
        ("features", "source_hash", "extractor_version", "model_name", "metadata"),
        row,
    ))


def save_book_features(
    work_id: str,
    features: dict,
    source_hash: str,
    extractor_version: str,
    model_name: str,
    metadata: dict | None = None,
):
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO book_features (
                    work_id, features, source_hash, extractor_version,
                    model_name, metadata
                ) VALUES (%s, %s, %s, %s, %s, COALESCE(%s::jsonb, '{}'::jsonb))
                ON CONFLICT (work_id) DO UPDATE SET
                    features = EXCLUDED.features,
                    source_hash = EXCLUDED.source_hash,
                    extractor_version = EXCLUDED.extractor_version,
                    model_name = EXCLUDED.model_name,
                    metadata = CASE WHEN %s THEN EXCLUDED.metadata
                                    ELSE book_features.metadata END,
                    updated_at = CURRENT_TIMESTAMP
                """,
                (
                    work_id, Jsonb(features), source_hash,
                    extractor_version, model_name,
                    Jsonb(metadata) if metadata is not None else None,
                    metadata is not None,
                ),
            )


def list_catalog(extractor_version: str, model_name: str) -> list[dict]:
    # Solo características de la misma versión y modelo, con título disponible.
    # Los registros manuales de las comprobaciones quedan fuera.
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT work_id, metadata, features
                FROM book_features
                WHERE extractor_version = %s AND model_name = %s
                  AND jsonb_typeof(metadata) = 'object'
                  AND COALESCE(metadata->>'title', '') <> ''
                ORDER BY work_id
                """,
                (extractor_version, model_name),
            )
            rows = cursor.fetchall()
    return [
        {"externalId": work_id, "metadata": metadata, "features": features}
        for work_id, metadata, features in rows
    ]
