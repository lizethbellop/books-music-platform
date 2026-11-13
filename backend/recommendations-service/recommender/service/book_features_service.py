import os
from pathlib import Path

from dotenv import load_dotenv

from ..database import get_connection

from ..book_features_repository import get_book_features, save_book_features
from ..book_metadata import prepare_book_metadata, calculate_source_hash
from ..books_client import get_book_metadata
from ..feature_extractor import (
    BookFeatures, EXTRACTOR_VERSION, extract_book_features,
)
from ..openlibrary import work_id as normalize_work_id


def get_or_extract_book_features(book: dict) -> BookFeatures:
    work_id = normalize_work_id(book.get("externalId", ""))
    if work_id is None:
        raise ValueError("El libro necesita un externalId válido de OpenLibrary")
    # Coordinate both automatic jobs and the explicit extraction endpoint.
    with get_connection() as connection:
        connection.execute("SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))", (work_id,))
        return _get_or_extract_book_features(book)


def _get_or_extract_book_features(book: dict) -> BookFeatures:
    work_id = normalize_work_id(book.get("externalId", ""))
    if work_id is None:
        raise ValueError("El libro necesita un externalId válido de OpenLibrary")

    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    model_name = os.environ["GROQ_MODEL"].strip()
    if not model_name:
        raise ValueError("Configura GROQ_MODEL")

    source = prepare_book_metadata(book)
    source_hash = calculate_source_hash(source)
    saved = get_book_features(work_id)
    if (
        saved is not None
        and saved["source_hash"] == source_hash
        and saved["extractor_version"] == EXTRACTOR_VERSION
        and saved["model_name"] == model_name
    ):
        features = BookFeatures.model_validate(saved["features"])
    else:
        features = extract_book_features(source)

    # Guarda también los datos públicos necesarios para las tarjetas del catálogo.
    # Incluso una extracción reutilizada puede completar metadatos que faltaban.
    metadata = {
        "title": source["title"],
        "authors": list(dict.fromkeys(
            name.strip() for name in (book.get("authors") or [])
            if isinstance(name, str) and name.strip()
        )),
        "coverUrl": book.get("coverUrl"),
        "firstPublishDate": book.get("firstPublishDate"),
    }
    save_book_features(
        work_id=work_id,
        features=features.model_dump(),
        source_hash=source_hash,
        extractor_version=EXTRACTOR_VERSION,
        model_name=model_name,
        metadata=metadata,
    )
    return features


def get_features_for_book(external_id: str, token: str) -> BookFeatures:
    return get_or_extract_book_features(get_book_metadata(external_id, token))
