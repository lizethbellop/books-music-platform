from uuid import uuid4

import pytest

from recommender.music_models import MusicRef
from recommender.resolve_music_catalog import CatalogResolver


def test_verifies_artist_by_explicit_spotify_link(tmp_path):
    resolver = CatalogResolver(cache_dir=tmp_path)
    mbid = str(uuid4())
    ref = MusicRef(contentType="ARTIST", externalId="artist")
    def mb(path, **params):
        return {"resource": params["resource"], "relations": [{"artist": {"id": mbid}}]}
    resolver.mb = mb
    try:
        assert resolver.resolve(ref) == (mbid, "https://open.spotify.com/artist/artist")
    finally:
        resolver.close()


def test_rejects_ambiguous_direct_links(tmp_path):
    resolver = CatalogResolver(cache_dir=tmp_path)
    resolver.mb = lambda path, **params: {"resource": params["resource"], "relations": [
        {"artist": {"id": str(uuid4())}}, {"artist": {"id": str(uuid4())}}]}
    try:
        assert resolver.resolve(MusicRef(contentType="ARTIST", externalId="artist"))[0] is None
    finally:
        resolver.close()


def test_album_edition_resolves_to_its_release_group(tmp_path):
    resolver = CatalogResolver(cache_dir=tmp_path)
    mbid = str(uuid4())
    def mb(path, **params):
        if path == "url":
            return {"resource": params["resource"], "relations": [{"release": {"id": "edition"}}]}
        assert path == "release/edition"
        return {"release-group": {"id": mbid}}
    resolver.mb = mb
    try:
        assert resolver.resolve(MusicRef(contentType="ALBUM", externalId="album"))[0] == mbid
    finally:
        resolver.close()


@pytest.mark.parametrize("count", [1, 2])
def test_isrc_fallback_requires_one_exact_recording(tmp_path, count):
    resolver = CatalogResolver(cache_dir=tmp_path)
    mbid = str(uuid4())
    resolver.spotify_isrc = lambda oid: "TEST12345678"
    def mb(path, **params):
        if path == "url": return None
        if path == "recording": return {"count": count, "recordings": [{"id": mbid}]}
        return {"id": mbid, "isrcs": ["TEST12345678"]}
    resolver.mb = mb
    try:
        result = resolver.resolve(MusicRef(contentType="SONG", externalId="track"))
        assert result[0] == (mbid if count == 1 else None)
    finally:
        resolver.close()
