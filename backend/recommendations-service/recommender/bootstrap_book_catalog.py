"""Catálogo inicial desde fuentes públicas; extracciones guardadas y reutilizables."""
import argparse
import json
from pathlib import Path
import time

import httpx

from .provider_budget import provider_request
from .book_features_repository import get_book_features
from .book_metadata import prepare_book_metadata, calculate_source_hash
from .feature_extractor import EXTRACTOR_VERSION
from .service.book_features_service import get_or_extract_book_features

STARTER_IDS = ["OL26350090W", "OL17797857W", "OL20150260W", "OL20354498W",
               "OL17203233W", "OL17765229W", "OL274505W"]
QUERIES = ["author:\"Ali Hazelwood\"", "author:\"Sally Rooney\"", "author:\"Jenny Han\""]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--max-books", type=int, default=12)
    args = parser.parse_args()
    if not 1 <= args.max_books <= 20:
        parser.error("Máximo de 1 a 20 libros")
    cache = Path("artifacts/openlibrary-cache")
    cache.mkdir(parents=True, exist_ok=True)
    client = httpx.Client(timeout=25, follow_redirects=True, headers={
        "User-Agent": "musa-recommendations/0.1 (https://github.com/lizethbellop/books-music-platform)"})
    last = [0.0]

    def get(path, params=None):
        import hashlib
        key = hashlib.sha256(json.dumps([path, params], sort_keys=True).encode()).hexdigest()
        file = cache / (key + ".json")
        if file.exists():
            return json.loads(file.read_text())
        wait = 3.1 - (time.monotonic() - last[0])
        if wait > 0:
            time.sleep(wait)
        last[0] = time.monotonic()
        response = provider_request(client, "openlibrary", "GET", "https://openlibrary.org" + path, params=params)
        response.raise_for_status()
        value = response.json()
        file.write_text(json.dumps(value, ensure_ascii=False))
        return value

    ids = list(STARTER_IDS)
    search_metadata = {}
    for query in QUERIES:
        try:
            result = get("/search.json", {"q": query, "limit": 3,
                "fields": "key,title,author_name,subject,cover_i,first_publish_year"})
            for row in result.get("docs", []):
                oid = row.get("key", "").removeprefix("/works/")
                if oid not in ids and oid.startswith("OL") and oid.endswith("W"):
                    ids.append(oid)
                search_metadata[oid] = row
        except httpx.HTTPError:
            print("Búsqueda adicional pendiente; continuamos con obras conocidas", flush=True)

    prepared, pending = [], []
    last_extraction = 0.0
    try:
        for oid in ids[:args.max_books]:
            try:
                work = get(f"/works/{oid}.json")
                authors = []
                for entry in work.get("authors", []):
                    author_key = entry.get("author", {}).get("key")
                    if author_key:
                        author = get(author_key + ".json")
                        if author.get("name"):
                            authors.append(author["name"])
                description = work.get("description") or ""
                if isinstance(description, dict):
                    description = description.get("value", "")
                covers = [v for v in work.get("covers", []) if isinstance(v, int) and v > 0]
                cover = covers[0] if covers else search_metadata.get(oid, {}).get("cover_i")
                book = {"externalId": oid, "title": work["title"], "authors": authors,
                        "subjects": work.get("subjects") or [], "description": description,
                        "coverUrl": f"https://covers.openlibrary.org/b/id/{cover}-M.jpg" if cover else None}
                saved = get_book_features(oid)
                needs_extraction = not (saved and saved["source_hash"] == calculate_source_hash(prepare_book_metadata(book))
                                        and saved["extractor_version"] == EXTRACTOR_VERSION)
                if needs_extraction:
                    wait = 35 - (time.monotonic() - last_extraction)
                    if wait > 0:
                        time.sleep(wait)
                    last_extraction = time.monotonic()
                get_or_extract_book_features(book)
                prepared.append({"externalId": oid, "title": book["title"],
                                 "sourceUrl": f"https://openlibrary.org/works/{oid}"})
                print(f"Preparado: {oid} — {book['title']}", flush=True)
            except (httpx.HTTPError, ValueError) as error:
                code = error.response.status_code if isinstance(error, httpx.HTTPStatusError) else type(error).__name__
                pending.append({"externalId": oid, "reason": str(code)})
                print(f"Pendiente: {oid} ({code})", flush=True)
    finally:
        client.close()
        Path("examples/book_catalog_sources.json").write_text(json.dumps({
            "prepared": prepared, "pending": pending}, ensure_ascii=False, indent=2))
    print(f"Libros preparados: {len(prepared)}; pendientes: {len(pending)}", flush=True)


if __name__ == "__main__":
    main()
