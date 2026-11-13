"""Características de fuente independiente; no envía datos de Spotify a modelos."""
import hashlib
import json
import os
from uuid import UUID

import httpx

from .music_models import MusicFeatures
from .provider_budget import provider_request

ENTITIES = {"ARTIST": "artist", "ALBUM": "release-group", "SONG": "recording"}
USER_AGENT = "musa-recommendations/0.1 (https://github.com/lizethbellop/books-music-platform)"


def parse_musicbrainz(content_type: str, musicbrainz_id: str, payload: dict):
    if payload.get("id") != musicbrainz_id:
        raise ValueError("MusicBrainz devolvió otra entidad")
    title = payload.get("name") if content_type == "ARTIST" else payload.get("title")
    if not isinstance(title, str) or not title.strip():
        raise ValueError("La entidad musical no tiene nombre")

    def labels(field):
        return sorted({
            row["name"].strip().casefold()[:120]
            for row in payload.get(field, [])
            if isinstance(row, dict) and isinstance(row.get("name"), str)
            and row["name"].strip() and (row.get("count") or 0) > 0
        })[:30]

    credits = [
        row["artist"] for row in payload.get("artist-credit", [])
        if isinstance(row, dict) and isinstance(row.get("artist"), dict)
    ]
    if content_type == "ARTIST":
        artists = [musicbrainz_id]
        names = [title.strip()]
    else:
        artists = sorted({str(UUID(a["id"])) for a in credits if a.get("id")})
        names = [a["name"] for a in credits if isinstance(a.get("name"), str)]

    features = MusicFeatures(genres=labels("genres"), tags=labels("tags"), artists=artists)
    entity = ENTITIES[content_type]
    metadata = {
        "name": title.strip(), "artistNames": names, "artistIds": artists,
        "releaseDate": payload.get("first-release-date"),
        "source": "musicbrainz",
        "sourceUrl": f"https://musicbrainz.org/{entity}/{musicbrainz_id}",
    }
    source_hash = hashlib.sha256(json.dumps(
        {"features": features.model_dump(), "metadata": metadata},
        sort_keys=True, ensure_ascii=False, separators=(",", ":"),
    ).encode()).hexdigest()
    return features, metadata, source_hash


def fetch_musicbrainz(content_type: str, musicbrainz_id: str):
    musicbrainz_id = str(UUID(musicbrainz_id))
    entity = ENTITIES[content_type]
    includes = "tags+genres" if content_type == "ARTIST" else "tags+genres+artist-credits"
    with httpx.Client(timeout=20) as client:
        response = provider_request(client, 'musicbrainz', 'GET',
            f"https://musicbrainz.org/ws/2/{entity}/{musicbrainz_id}",
            params={"fmt": "json", "inc": includes},
            headers={"User-Agent": os.environ.get("MUSICBRAINZ_USER_AGENT", USER_AGENT)},
        )
        response.raise_for_status()
        payload = response.json()
    return parse_musicbrainz(content_type, musicbrainz_id, payload)
