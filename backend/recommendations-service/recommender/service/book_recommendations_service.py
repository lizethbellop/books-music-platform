import os
from pathlib import Path

from dotenv import load_dotenv

from ..book_features_repository import list_catalog
from ..feature_extractor import EXTRACTOR_VERSION
from .book_knn import recommend_from_catalog
from .book_catalog_jobs import enqueue_books
from .book_tastes import BookTastes, get_book_tastes
from ..openlibrary import work_id as normalize_work_id


def recommend_books_for_user(token: str, limit: int = 12) -> dict:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    tastes = get_book_tastes(token)
    catalog = list_catalog(EXTRACTOR_VERSION, os.environ["GROQ_MODEL"].strip())
    result = recommend_from_catalog(catalog, tastes, limit)
    result["preparation"] = enqueue_books(tastes.seeds, [b["externalId"] for b in catalog], token)
    return result


def recommend_similar_books(external_id: str, limit: int = 8, token: str | None = None) -> dict:
    """Use one book as reference; no personal signals or provider calls."""
    work_id = normalize_work_id(external_id)
    if work_id is None:
        raise ValueError("Identificador de OpenLibrary inválido")
    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    catalog = list_catalog(EXTRACTOR_VERSION, os.environ["GROQ_MODEL"].strip())
    result = recommend_from_catalog(
        catalog, BookTastes(seeds={work_id: 1.0}, exclude={work_id}), limit,
    )
    if result["mode"] == "profile_not_prepared":
        result["mode"] = "reference_not_prepared"
    result["preparation"] = enqueue_books([work_id], [b["externalId"] for b in catalog], token)
    result["reference_book_id"] = work_id
    return result
