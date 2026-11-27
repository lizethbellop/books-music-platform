import os
from dataclasses import dataclass
from pathlib import Path

import httpx
from dotenv import load_dotenv
from pydantic import BaseModel, Field, ConfigDict

from ..openlibrary import work_id as normalize_work_id


class ProfileSeeds(BaseModel):
    model_config = ConfigDict(extra="ignore")
    bookIds: list[str]


class BookRating(BaseModel):
    model_config = ConfigDict(extra="ignore")
    externalId: str
    rating: float = Field(ge=1, le=5)


class BookSignals(BaseModel):
    model_config = ConfigDict(extra="ignore")
    ratings: list[BookRating]
    libraryIds: list[str]
    favoriteIds: list[str] = Field(default_factory=list)


@dataclass
class BookTastes:
    seeds: dict[str, float]
    exclude: set[str]


def normalize_ids(values) -> set[str]:
    return {
        oid for value in values
        if (oid := normalize_work_id(value)) is not None
    }


def build_tastes(profile: ProfileSeeds, signals: BookSignals) -> BookTastes:
    # Profile ya reúne listas y preferencias BOOK, sin duplicados.
    listed = normalize_ids(profile.bookIds)
    library = normalize_ids(signals.libraryIds)
    favorites = normalize_ids(signals.favoriteIds)
    ratings = {}
    for rating in signals.ratings:
        oid = normalize_work_id(rating.externalId)
        if oid is not None:
            if oid in ratings:
                raise ValueError("El servicio devolvió calificaciones duplicadas")
            ratings[oid] = rating.rating

    seeds = {oid: 1.0 for oid in listed}
    for oid in favorites:
        seeds[oid] = max(seeds.get(oid, 0), 1.5)
    for oid, rating in ratings.items():
        if rating >= 4:
            # Pesos iniciales, aún no optimizados: 4 -> 2; 5 -> 3.
            seeds[oid] = rating - 2
        else:
            # Una nota inferior a 4 prevalece sobre listas/preferencias.
            seeds.pop(oid, None)

    # Se excluyen todos los conocidos, no solo las semillas positivas.
    return BookTastes(
        seeds=seeds,
        exclude=listed | library | favorites | set(ratings),
    )


def get_book_tastes(token: str) -> BookTastes:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    headers = {"Authorization": f"Bearer {token}"}
    profile_url = os.environ["PROFILE_SERVICE_URL"].rstrip("/")
    books_url = os.environ["BOOKS_SERVICE_URL"].rstrip("/")

    # No se cachean gustos, tokens ni respuestas personales.
    # Los fallos NO se convierten en un perfil vacío.
    with httpx.Client(timeout=10, headers=headers) as client:
        response = client.get(profile_url + "/api/profiles/me/lists/book-seeds")
        response.raise_for_status()
        profile = ProfileSeeds.model_validate(response.json())

        response = client.get(books_url + "/books/recommendation-signals/me")
        response.raise_for_status()
        signals = BookSignals.model_validate(response.json())

    return build_tastes(profile, signals)
