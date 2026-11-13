import base64
import secrets
import time
from uuid import uuid4

import pytest
import jwt
from fastapi.testclient import TestClient

from recommender.api import create_app
from recommender.music_models import MusicProfile, MusicSignals
from recommender.musicbrainz_client import parse_musicbrainz
from recommender.prepare_music_catalog import read_mapping
from recommender.service.music_tastes import build_music_tastes, MusicTastes
from recommender.service.music_knn import recommend_music


def ref(kind, oid):
    return {"contentType": kind, "externalId": oid}


def item(kind, oid, genres=None, tags=None, artists=None):
    return {"key": f"{kind}:{oid}", "contentType": kind, "externalId": oid,
            "metadata": {"name": oid, "artistNames": artists or [oid],
                         "sourceUrl": "https://musicbrainz.org/artist/example"},
            "features": {"genres": genres or [], "tags": tags or [], "artists": artists or []}}


def test_ratings_override_lists_and_favorites_and_all_known_items_are_excluded():
    profile = MusicProfile(items=[ref("SONG", "same"), ref("ARTIST", "same"), ref("ALBUM", "album")])
    signals = MusicSignals(
        ratings=[dict(ref("SONG", "same"), rating=2), dict(ref("ALBUM", "album"), rating=5)],
        favorites=[ref("SONG", "same"), ref("SONG", "favorite")],
        reviewed=[ref("SONG", "reviewed")],
    )
    tastes = build_music_tastes(profile, signals)
    assert tastes.seeds == {"ARTIST:same": 1, "ALBUM:album": 3, "SONG:favorite": 1.5}
    assert tastes.exclude == {"SONG:same", "ARTIST:same", "ALBUM:album", "SONG:favorite", "SONG:reviewed"}


@pytest.mark.parametrize("kind", ["SONG", "ALBUM", "ARTIST"])
def test_artist_seed_can_find_all_three_types_without_mixing_results(kind):
    catalog = [item("ARTIST", "seed", genres=["dream pop"]),
               item("SONG", "track", genres=["dream pop"]),
               item("ALBUM", "record", genres=["dream pop"]),
               item("ARTIST", "other", genres=["dream pop"])]
    result = recommend_music(catalog, MusicTastes({"ARTIST:seed": 1}, {"ARTIST:seed"}), kind)
    assert len(result["items"]) == 1
    assert result["items"][0]["contentType"] == kind
    assert result["items"][0]["sourceItemKey"] == "ARTIST:seed"


def test_same_id_in_another_type_is_not_accidentally_excluded():
    catalog = [item("ARTIST", "same", genres=["synth-pop"]), item("SONG", "same", genres=["synth-pop"])]
    result = recommend_music(catalog, MusicTastes({"ARTIST:same": 1}, {"ARTIST:same"}), "SONG")
    assert result["items"][0]["spotifyId"] == "same"


def test_empty_catalog_and_missing_seeds_are_explicit():
    tastes = MusicTastes({"ARTIST:seed": 1}, {"ARTIST:seed"})
    assert recommend_music([], tastes, "ALBUM")["mode"] == "catalog_not_ready"
    result = recommend_music([item("ALBUM", "other", genres=["pop"])], tastes, "ALBUM")
    assert result["mode"] == "profile_not_prepared" and not result["items"]


def test_cold_start_is_not_presented_as_personalized():
    result = recommend_music([item("SONG", "one", genres=["pop"])], MusicTastes({}, set()), "SONG")
    assert result["mode"] == "explore"
    assert result["items"][0]["score"] == 0


def test_no_similarities_returns_no_invented_recommendations():
    catalog = [item("ARTIST", "seed", genres=["jazz"]), item("SONG", "other", genres=["metal"])]
    assert recommend_music(catalog, MusicTastes({"ARTIST:seed": 1}, {"ARTIST:seed"}), "SONG")["items"] == []


def test_musicbrainz_metadata_is_not_confused_with_spotify_metadata():
    mbid = str(uuid4())
    features, metadata, digest = parse_musicbrainz("ARTIST", mbid, {
        "id": mbid, "name": "Example",
        "genres": [{"name": "Dream Pop", "count": 3}],
        "tags": [{"name": "Alternative", "count": 1}, {"name": "Rejected", "count": -1}],
    })
    assert features.genres == ["dream pop"] and features.tags == ["alternative"]
    assert features.artists == [mbid]
    assert metadata["source"] == "musicbrainz" and len(digest) == 64
    assert "Example" not in str(features.model_dump())


def test_unverified_catalog_mapping_is_rejected(tmp_path):
    path = tmp_path / "mapping.csv"
    path.write_text(f"content_type,spotify_id,musicbrainz_id,verified\nSONG,one,{uuid4()},false\n")
    with pytest.raises(ValueError, match="verificada"):
        read_mapping(path)


def test_mismatching_musicbrainz_record_is_rejected():
    with pytest.raises(ValueError, match="otra entidad"):
        parse_musicbrainz("SONG", str(uuid4()), {"id": str(uuid4()), "title": "Wrong"})


@pytest.mark.parametrize("kind", ["songs", "albums", "artists"])
def test_music_api_requires_auth_and_passes_the_right_type(monkeypatch, kind):
    monkeypatch.delenv("MODEL_PATH", raising=False)
    key = secrets.token_bytes(64)
    token = jwt.encode(dict(sub=str(uuid4()), exp=time.time()+60, iss="musa-auth", aud="musa-api"), key, algorithm="HS512")
    calls = []
    def recommend(token, content_type, limit):
        calls.append((token, content_type, limit))
        return {"mode": "knn_content", "items": []}
    monkeypatch.setattr("recommender.api.recommend_music_for_user", recommend)
    app = create_app(secret=base64.b64encode(key).decode())
    with TestClient(app) as client:
        path = f"/api/recommendations/music/{kind}/me"
        assert client.get(path).status_code == 401
        response = client.get(path + "?limit=4", headers={"Authorization": f"Bearer {token}"})
        assert response.status_code == 200
        assert response.headers["Cache-Control"] == "no-store"
    expected = {"songs": "SONG", "albums": "ALBUM", "artists": "ARTIST"}[kind]
    assert calls == [(token, expected, 4)]


def test_pop_alone_and_chart_tags_do_not_create_similarity():
    catalog=[item('SONG','seed',genres=['pop','synth-pop'],tags=['billboard hot 100 2026']),
             item('SONG','chart',tags=['billboard hot 100 2026']),
             item('SONG','broad',genres=['pop']),
             item('SONG','specific',genres=['synth pop'])]
    result=recommend_music(catalog,MusicTastes({'SONG:seed':3},{'SONG:seed'}),'SONG')
    assert [row['spotifyId'] for row in result['items']] == ['specific']


def test_repeated_genre_is_not_counted_again_as_a_tag():
    from recommender.service.music_knn import music_vector
    vector=music_vector({'genres':['synth-pop'],'tags':['synthpop','offizielle charts'],'artists':[]})
    assert set(vector) == {'genres:synth pop'}


def test_diversity_does_not_fill_remaining_space_with_same_artist():
    catalog=[item('ARTIST','seed',genres=['synth-pop'])]
    catalog += [item('SONG',f'song-{i}',genres=['synth-pop'],artists=['one-artist']) for i in range(5)]
    catalog += [item('SONG','other',genres=['synth-pop'],artists=['another-artist'])]
    result=recommend_music(catalog,MusicTastes({'ARTIST:seed':1},{'ARTIST:seed'}),'SONG')
    assert len(result['items']) == 3
    assert sum(row['artistNames']==['one-artist'] for row in result['items']) == 2


def test_poorly_tagged_same_artist_can_still_be_recommended():
    catalog=[item('SONG','seed',genres=['synth-pop'],artists=['artist-id']),
             item('ALBUM','other',artists=['artist-id'])]
    result=recommend_music(catalog,MusicTastes({'SONG:seed':3},{'SONG:seed'}),'ALBUM')
    assert result['items'][0]['name'] == 'other'
    assert result['items'][0]['reason'].startswith('Comparte artista')


def test_duplicate_titles_from_same_artist_do_not_repeat():
    catalog=[item('SONG','seed',genres=['synth-pop'])]
    a=item('SONG','edition-a',genres=['synth-pop'],artists=['artist-id'])
    b=item('SONG','edition-b',genres=['synth-pop'],artists=['artist-id'])
    a['metadata']['name']=b['metadata']['name']='Same recording'
    result=recommend_music(catalog+[a,b],MusicTastes({'SONG:seed':1},{'SONG:seed'}),'SONG')
    assert len(result['items']) == 1


def test_verified_artist_affinity_is_explicit_and_reason_deduplicated():
    catalog=[item('SONG','liked',genres=['synth-pop'],artists=['same-artist']),
             item('ARTIST','artist',genres=['synth-pop'],tags=['synthpop'],artists=['same-artist'])]
    result=recommend_music(catalog,MusicTastes({'SONG:liked':3},{'SONG:liked'}),'ARTIST')
    assert result['items'][0]['score']==1
    assert result['items'][0]['reason'].count('synth pop')==1
    assert 'artista y' in result['items'][0]['reason']
