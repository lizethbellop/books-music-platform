"""Exporta solo el catálogo público preparado; nunca perfiles, claves ni tokens."""
import argparse
import os
from pathlib import Path

from dotenv import load_dotenv
from psycopg import sql
from psycopg.types.json import Jsonb

from .database import get_connection
from .feature_extractor import EXTRACTOR_VERSION
from .music_models import MUSIC_FEATURE_VERSION


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="database/seed_catalog.sql")
    args = parser.parse_args()
    load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=False)
    statements = ["-- Catálogo inicial público; ejecutar después de setup.sql/migraciones.\nBEGIN;\n"]
    count = 0
    with get_connection() as connection:
        with connection.cursor() as cursor:
            specifications = [
                ("book_features", ["work_id", "features", "metadata", "source_hash", "extractor_version", "model_name"],
                 ["work_id"], "extractor_version = %s AND model_name = %s AND COALESCE(metadata->>'title', '') <> ''",
                 (EXTRACTOR_VERSION, os.environ["GROQ_MODEL"])),
                ("music_features", ["content_type", "external_id", "musicbrainz_id", "features", "metadata", "source_hash", "source_name", "feature_version", "mapping_verified"],
                 ["content_type", "external_id"], "feature_version = %s AND mapping_verified = true",
                 (MUSIC_FEATURE_VERSION,)),
            ]
            for table, columns, keys, where, params in specifications:
                cursor.execute(sql.SQL("SELECT {} FROM {} WHERE {} ORDER BY {}").format(
                    sql.SQL(", ").join(map(sql.Identifier, columns)), sql.Identifier(table),
                    sql.SQL(where), sql.SQL(", ").join(map(sql.Identifier, keys))), params)
                for row in cursor.fetchall():
                    conflict_keys = ['content_type','musicbrainz_id'] if table=='music_features' else keys
                    assignments = sql.SQL(", ").join(
                        sql.SQL("{} = EXCLUDED.{}").format(sql.Identifier(name), sql.Identifier(name))
                        for name in columns if name not in set(keys) | set(conflict_keys))
                    statement = sql.SQL("INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO UPDATE SET {}, updated_at = CURRENT_TIMESTAMP;\n").format(
                        sql.Identifier(table), sql.SQL(", ").join(map(sql.Identifier, columns)),
                        sql.SQL(", ").join(sql.Literal(Jsonb(v) if isinstance(v, dict) else v) for v in row),
                        sql.SQL(", ").join(map(sql.Identifier, conflict_keys)), assignments)
                    statements.append(statement.as_string(connection))
                    count += 1
                    if table=='music_features':
                        kind, oid, mbid = row[:3]
                        alias=sql.SQL("INSERT INTO music_feature_aliases(content_type,external_id,canonical_external_id) SELECT {kind},{oid},external_id FROM music_features WHERE content_type={kind} AND musicbrainz_id={mbid} AND external_id<>{oid} ON CONFLICT(content_type,external_id) DO UPDATE SET canonical_external_id=EXCLUDED.canonical_external_id;\n").format(
                            kind=sql.Literal(kind),oid=sql.Literal(oid),mbid=sql.Literal(mbid))
                        statements.append(alias.as_string(connection))
        aliases=connection.execute("""SELECT a.content_type,a.external_id,f.musicbrainz_id
            FROM music_feature_aliases a JOIN music_features f
            ON f.content_type=a.content_type AND f.external_id=a.canonical_external_id
            WHERE f.feature_version=%s AND f.mapping_verified=true
            ORDER BY a.content_type,a.external_id""",(MUSIC_FEATURE_VERSION,)).fetchall()
        for kind, oid, mbid in aliases:
            statement=sql.SQL("INSERT INTO music_feature_aliases(content_type,external_id,canonical_external_id) SELECT {kind},{oid},external_id FROM music_features WHERE content_type={kind} AND musicbrainz_id={mbid} AND external_id<>{oid} ON CONFLICT(content_type,external_id) DO UPDATE SET canonical_external_id=EXCLUDED.canonical_external_id;\n").format(
                kind=sql.Literal(kind),oid=sql.Literal(oid),mbid=sql.Literal(mbid))
            statements.append(statement.as_string(connection))
    statements.append("COMMIT;\n")
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(statements))
    print(f"Exportados {count} elementos públicos a {path}")


if __name__ == "__main__":
    main()
