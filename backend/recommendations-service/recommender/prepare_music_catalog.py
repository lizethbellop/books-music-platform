"""Importa correspondencias verificadas. No necesita login ni clave de Groq."""
import argparse
import csv
from pathlib import Path
import time
from uuid import UUID

import httpx
from dotenv import load_dotenv

from .music_models import MUSIC_FEATURE_VERSION, MusicRef
from .music_features_repository import get_music_features, save_music_features
from .musicbrainz_client import fetch_musicbrainz


def read_mapping(path):
    with open(path, encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    validated = []
    for row in rows:
        if row.get("verified", "").strip().lower() != "true":
            raise ValueError("Cada correspondencia debe estar verificada: verified=true")
        ref = MusicRef(contentType=row["content_type"], externalId=row["spotify_id"])
        mbid = str(UUID(row["musicbrainz_id"]))
        validated.append((ref, mbid))
    if len({ref.key for ref, _ in validated}) != len(validated):
        raise ValueError("Hay correspondencias duplicadas")
    if len({(ref.contentType, mbid) for ref, mbid in validated}) != len(validated):
        raise ValueError("Dos IDs apuntan a la misma entidad musical; revisar duplicados")
    return validated


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mapping", required=True, help="CSV de correspondencias verificadas")
    parser.add_argument("--max-items", type=int, default=10)
    parser.add_argument("--refresh", action="store_true")
    args = parser.parse_args()
    if not 1 <= args.max_items <= 50:
        parser.error("--max-items debe estar entre 1 y 50")
    rows = read_mapping(args.mapping)
    if len(rows) > args.max_items:
        parser.error("El CSV supera --max-items; dividirlo o aumentar el límite hasta 50")
    load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=False)
    for ref, mbid in rows:
        cached = get_music_features(ref.contentType, ref.externalId)
        if cached and cached["musicbrainz_id"] != mbid:
            raise ValueError("Cambió una correspondencia existente; revisarla antes de reemplazarla")
        if cached and cached["feature_version"] == MUSIC_FEATURE_VERSION and not args.refresh:
            print(f"Reutilizado: {ref.key}")
            continue
        # Ejecutar una sola importación a la vez. Se respeta más de 1 s entre consultas.
        time.sleep(1.1)
        features, metadata, source_hash = fetch_musicbrainz(ref.contentType, mbid)
        save_music_features(ref.contentType, ref.externalId, mbid,
                            features.model_dump(), metadata, source_hash)
        print(f"Preparado: {ref.key} — {metadata['name']}")


if __name__ == "__main__":
    try:
        main()
    except httpx.HTTPStatusError as error:
        print(f"Importación detenida: HTTP {error.response.status_code}. Lo ya guardado se conserva.")
        raise SystemExit(1) from None
