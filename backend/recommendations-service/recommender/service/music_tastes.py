import os
from dataclasses import dataclass
from pathlib import Path

import httpx
from dotenv import load_dotenv

from ..music_models import MusicProfile, MusicSignals


@dataclass
class MusicTastes:
    seeds: dict[str, float]
    exclude: set[str]


def build_music_tastes(profile: MusicProfile, signals: MusicSignals) -> MusicTastes:
    listed = {ref.key for ref in profile.items}
    favorites = {ref.key for ref in signals.favorites}
    reviewed = {ref.key for ref in signals.reviewed}
    seeds = {key: 1.0 for key in listed}
    # Los favoritos explícitos son una señal adicional, sin duplicar entradas.
    for key in favorites:
        seeds[key] = max(seeds.get(key, 0), 1.5)
    rated = set()
    for rating in signals.ratings:
        if rating.key in rated:
            raise ValueError("Calificaciones musicales duplicadas")
        rated.add(rating.key)
        if rating.rating >= 4:
            seeds[rating.key] = rating.rating - 2
        else:
            seeds.pop(rating.key, None)
    return MusicTastes(seeds, listed | favorites | reviewed | rated)


def get_music_tastes(token: str) -> MusicTastes:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    with httpx.Client(timeout=10, headers={"Authorization": f"Bearer {token}"}) as client:
        response = client.get(os.environ["PROFILE_SERVICE_URL"].rstrip("/") + "/api/profiles/me/music-seeds")
        response.raise_for_status()
        profile = MusicProfile.model_validate(response.json())
        response = client.get(os.environ.get("MUSIC_SERVICE_URL", "http://localhost:8080").rstrip("/") + "/api/music/recommendation-signals/me")
        response.raise_for_status()
        signals = MusicSignals.model_validate(response.json())
    return build_music_tastes(profile, signals)
