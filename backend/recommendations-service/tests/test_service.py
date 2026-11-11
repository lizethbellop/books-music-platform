import base64
import time
from uuid import uuid4

import jwt
import pandas as pd
import pytest
from fastapi.testclient import TestClient

from recommender.data import load, split
from recommender.model import fit
from recommender.artifacts import save, load as load_artifact
from recommender.api import create_app
from recommender.train import evaluate


@pytest.fixture
def dataset():
    catalog = pd.DataFrame(dict(item_id=list("abcdef"), title=["Space one", "Space two", "Poetry one", "Poetry two", "History one", "New history"], authors=["A"]*6, tags=["space", "space", "poetry", "poetry", "history", "history"]))
    ratings = pd.DataFrame([(u, i, 5) for u, ids in [("1", "abcd"), ("2", "abce"), ("3", "bcde")] for i in ids], columns=["user_id", "item_id", "rating"])
    return catalog, ratings


def test_split_and_reproducibility(dataset):
    catalog, ratings = dataset
    a, b, c = split(ratings)
    pairs = lambda x: set(zip(x.user_id, x.item_id))
    assert not pairs(a) & pairs(b) and not pairs(a) & pairs(c) and not pairs(b) & pairs(c)
    assert len(a)+len(b)+len(c) == len(ratings)
    assert split(ratings)[0].equals(a)
    model = fit(catalog, a)
    assert model.popularity[-1] == 0  # held-out/new item does not affect counts
    assert model.recommend(["a"], ["b"]) == fit(catalog, a).recommend(["a"], ["b"])
    assert all(r["item_id"] not in {"a", "b"} for r in model.recommend(["a"], ["b"]))
    assert evaluate(model, a, b, "hybrid")["users"] == 3


def test_artifacts_and_api(tmp_path, dataset):
    catalog, ratings = dataset
    path = tmp_path / "v1"
    save(fit(catalog, ratings), path, dict(version="v1", domain="books", namespace="fixture", algorithm="hybrid"))
    model, _ = load_artifact(path)
    assert model.ids == list("abcdef")
    key = b"x"*64
    def token(**updates):
        claims = dict(sub=str(uuid4()), exp=time.time()+60, iss="musa-auth", aud="musa-api")
        claims.update(updates)
        return {"Authorization": "Bearer " + jwt.encode(claims, key, algorithm="HS512")}
    with TestClient(create_app(path, base64.b64encode(key).decode())) as client:
        assert client.get("/health/ready").status_code == 200
        assert client.post("/api/recommendations/books", json={}).status_code == 401
        for updates in [dict(exp=0), dict(aud="wrong"), dict(iss="wrong"), dict(sub="1")]:
            assert client.post("/api/recommendations/books", headers=token(**updates), json={}).status_code == 401
        response = client.post("/api/recommendations/books", headers=token(), json={"seeds": ["a"], "exclude": ["b"], "limit": 2})
        assert response.status_code == 200
        assert len(response.json()["items"]) == 2
        assert client.post("/api/recommendations/books", headers=token(), json={"limit": 101}).status_code == 422
        assert client.post("/api/recommendations/books", headers=token(), json={"seeds": ["unknown"]}).status_code == 422
        assert client.post("/api/recommendations/music", headers=token(), json={}).status_code == 404
    (path / "catalog.json").write_text("tampered")
    with pytest.raises(ValueError, match="checksum"):
        load_artifact(path)


def test_data_validation_and_duplicate_pairs(tmp_path, dataset):
    catalog, ratings = dataset
    catalog.to_csv(tmp_path / "catalog.csv", index=False)
    pd.concat([ratings, ratings.iloc[:1]]).to_csv(tmp_path / "ratings.csv", index=False)
    _, clean = load(tmp_path / "catalog.csv", tmp_path / "ratings.csv")
    assert len(clean) == len(ratings)
    ratings["rating"] = ratings.rating.astype(float)
    ratings.loc[0, "rating"] = float("inf")
    ratings.to_csv(tmp_path / "ratings.csv", index=False)
    with pytest.raises(ValueError):
        load(tmp_path / "catalog.csv", tmp_path / "ratings.csv")


def test_cold_start_and_music(tmp_path, dataset):
    catalog, ratings = dataset
    # f has no interactions; content still recognizes its shared history metadata.
    model = fit(catalog, ratings)
    assert model.explicit_factors[-1].sum() == 0
    result = model.recommend(["e"], algorithm="hybrid")
    cold = next(r for r in result if r["item_id"] == "f")
    assert cold["score"] > 0 and cold["reason"] == "hybrid"
    assert model.recommend([])[0]["reason"] == "popularity"
    path = tmp_path / "music-v1"
    save(model, path, dict(version="music-v1", domain="music", namespace="spotify-track", algorithm="hybrid"))
    with pytest.raises(FileExistsError):
        save(model, path, {})
    key = b"x"*64
    claims = dict(sub=str(uuid4()), exp=time.time()+60, iss="musa-auth", aud=["musa-api"])
    with TestClient(create_app(path, base64.b64encode(key).decode())) as client:
        valid = {"Authorization": "Bearer "+jwt.encode(claims, key, algorithm="HS512")}
        assert client.post("/api/recommendations/music", headers=valid, json={}).status_code == 200
        for token in [jwt.encode(claims, b"y"*64, algorithm="HS512"), jwt.encode(claims, key, algorithm="HS256")]:
            assert client.post("/api/recommendations/music", headers={"Authorization": "Bearer "+token}, json={}).status_code == 401


def test_cli(tmp_path, monkeypatch):
    import sys
    import json
    from pathlib import Path
    from recommender.train import main
    examples = Path(__file__).parents[1] / "examples"
    output = tmp_path / "model-v1"
    monkeypatch.setattr(sys, "argv", ["train", "--catalog", str(examples/"catalog.csv"),
                                    "--ratings", str(examples/"ratings.csv"), "--namespace", "fixture",
                                    "--output", str(output), "--include-nmf"])
    main()
    model, manifest = load_artifact(output)
    assert manifest["split"] == dict(train=6, validation=3, test=3)
    assert set(manifest["validation"]) == {"popularity", "knn", "mf", "nmf", "hybrid"}
    assert manifest["test"]["users"] == 3
    assert len(manifest["code_sha256"]) == 64
