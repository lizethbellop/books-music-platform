from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

MusicType = Literal["SONG", "ALBUM", "ARTIST"]
MUSIC_FEATURE_VERSION = "musicbrainz-tags-v1"
MUSIC_KINDS = {"songs": "SONG", "albums": "ALBUM", "artists": "ARTIST"}


class MusicRef(BaseModel):
    model_config = ConfigDict(extra="ignore")
    contentType: MusicType
    externalId: str = Field(min_length=1, max_length=255, pattern=r"^[A-Za-z0-9_-]+$")

    @property
    def key(self) -> str:
        return f"{self.contentType}:{self.externalId}"


class MusicRating(MusicRef):
    rating: float = Field(ge=1, le=5)


class MusicFeatures(BaseModel):
    model_config = ConfigDict(extra="forbid")
    genres: list[str]
    tags: list[str]
    artists: list[str]


class MusicProfile(BaseModel):
    items: list[MusicRef]


class MusicSignals(BaseModel):
    ratings: list[MusicRating]
    favorites: list[MusicRef]
    reviewed: list[MusicRef]
