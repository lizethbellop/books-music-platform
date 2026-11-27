import os
from pathlib import Path

import httpx
from dotenv import load_dotenv

from .openlibrary import work_id as normalize_work_id


def get_book_metadata(external_id: str, token: str) -> dict:
    work_id = normalize_work_id(external_id)

    if work_id is None:
        raise ValueError("Identificador de OpenLibrary inválido")

    if not token.strip():
        raise ValueError("Se necesita un token de sesión")

    env_path = Path(__file__).resolve().parent.parent / ".env"
    load_dotenv(env_path, override=False)

    base_url = os.environ["BOOKS_SERVICE_URL"].rstrip("/")

    with httpx.Client(timeout=20) as client:
        response = client.get(
            f"{base_url}/books/{work_id}",
            headers={
                "Authorization": f"Bearer {token}",
            },
        )

        response.raise_for_status()
        book = response.json()

    if not isinstance(book, dict):
        raise ValueError("El servicio de libros devolvió un formato inesperado")

    if book.get("externalId") != work_id:
        raise ValueError("El libro recibido no coincide con el solicitado")

    if not isinstance(book.get("title"), str) or not book["title"].strip():
        raise ValueError("El libro recibido no tiene título")

    return book