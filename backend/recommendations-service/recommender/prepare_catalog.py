"""Preparación manual del catálogo; nunca se ejecuta al abrir Home."""
import argparse
from getpass import getpass
import os
from pathlib import Path
import time

import httpx
from dotenv import load_dotenv

from .books_client import get_book_metadata
from .openlibrary import work_id as normalize_work_id
from .service.book_features_service import get_or_extract_book_features


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ids", nargs="+", required=True)
    parser.add_argument("--query", help="Búsqueda opcional de candidatos en Books/OpenLibrary")
    parser.add_argument("--max-books", type=int, default=5)
    parser.add_argument("--delay", type=float, default=20)
    args = parser.parse_args()
    if not 1 <= args.max_books <= 20 or args.delay < 0:
        parser.error("Usa entre 1 y 20 libros y una espera no negativa")
    if args.query and len(args.query) > 1000:
        parser.error("La búsqueda no puede superar 1000 caracteres")

    ids = []
    for value in args.ids:
        oid = normalize_work_id(value)
        if oid is None:
            parser.error(f"ID inválido: {value}")
        if oid not in ids:
            ids.append(oid)
    if len(ids) > args.max_books:
        parser.error("Los IDs explícitos superan --max-books")

    token = getpass("Token vigente, sin Bearer: ").strip()
    if not token:
        parser.error("Se requiere un token de sesión")
    load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=False)

    if args.query:
        with httpx.Client(timeout=20) as client:
            response = client.get(
                os.environ["BOOKS_SERVICE_URL"].rstrip("/") + "/books/recommendation-candidates",
                params={"q": args.query},
                headers={"Authorization": f"Bearer {token}"},
            )
            response.raise_for_status()
            candidates = response.json()
        if not isinstance(candidates, list):
            raise ValueError("Respuesta de candidatos inesperada")
        for book in candidates:
            oid = normalize_work_id(book.get("externalId", ""))
            if oid is not None and oid not in ids and len(ids) < args.max_books:
                ids.append(oid)

    print(f"Se prepararán como máximo {len(ids)} libros. Puede consumir cuota de Groq.")
    # Secuencial, sin reintentos automáticos. Una sola llamada de extracción por
    # libro si no está vigente; la espera reduce ráfagas pero no garantiza cuotas.
    for position, oid in enumerate(ids):
        if position:
            time.sleep(args.delay)
        book = get_book_metadata(oid, token)
        get_or_extract_book_features(book)
        print(f"Preparado: {oid} — {book['title']}")


if __name__ == "__main__":
    try:
        main()
    except httpx.HTTPStatusError as error:
        print(f"Preparación detenida: HTTP {error.response.status_code}.")
        print("Lo ya guardado se conserva. Repite después; las extracciones vigentes se reutilizan.")
        raise SystemExit(1) from None
