import base64
import time
from uuid import uuid4

import httpx
import jwt
import pytest
from fastapi.testclient import TestClient

from recommender.api import create_app


KEY = b"home-tests-only-"*5


def headers(user=None):
    claims = dict(sub=str(user or uuid4()), exp=time.time()+60, iss="musa-auth", aud=["musa-api"])
    return {"Authorization": "Bearer "+jwt.encode(claims, KEY, algorithm="HS512")}


def book(oid, title="Ocean worlds", author="Ada", subjects=None):
    return dict(externalId=oid, title=title, authors=[author], subjects=subjects or ["Science fiction"], coverUrl=None)


def client(handler, monkeypatch):
    monkeypatch.delenv("MODEL_PATH", raising=False)
    return TestClient(create_app(secret=base64.b64encode(KEY).decode(), upstream_transport=httpx.MockTransport(handler)))


def test_list_based_home_supports_unmapped_works_and_forwards_bearer(monkeypatch):
    auth = headers()
    calls = []
    def upstream(request):
        calls.append(request.url.path)
        assert request.headers["Authorization"] == auth["Authorization"]
        assert "x-user-id" not in request.headers
        if request.url.path.endswith("book-seeds"):
            assert "userId" not in request.url.query.decode()
            return httpx.Response(200, json={"bookIds": ["OL999W", "OL999W"]})
        if request.url.path == "/books/OL999W":
            return httpx.Response(200, json=book("OL999W"))
        if request.url.path.endswith("recommendation-candidates"):
            assert 'subject:"Science fiction"' in request.url.params["q"]
            return httpx.Response(200, json=[book("OL999W"), book("OL777W"), book("OL777W"), book("OL2M")])
        raise AssertionError(request.url)
    with client(upstream, monkeypatch) as api:
        result = api.get("/api/recommendations/books/me", headers=dict(auth, **{"X-User-Id": str(uuid4())}))
        assert result.status_code == 200
        body = result.json()
        assert body["mode"] == "content" and body["seed_count"] == 1
        assert [b["externalId"] for b in body["items"]] == ["OL777W"]
        assert "Ocean worlds" in body["items"][0]["reason"]
        assert result.headers["Cache-Control"] == "no-store"
        assert "Authorization" in result.headers["Vary"]
        assert len(calls) == 3


def test_no_lists_has_explicit_explore_state(monkeypatch):
    def upstream(request):
        if request.url.path.endswith("book-seeds"):
            return httpx.Response(200, json={"bookIds": []})
        assert request.url.path == "/books/explore"
        return httpx.Response(200, json=[book("OL1W"), book("OL1W"), book("OL2W")])
    with client(upstream, monkeypatch) as api:
        body = api.get("/api/recommendations/books/me?limit=1", headers=headers()).json()
        assert body["mode"] == "explore" and body["seed_count"] == 0
        assert len(body["items"]) == 1
        assert "guardaste" not in body["items"][0]["reason"]


def test_authentication_and_limit_rejected_before_calling_sources(monkeypatch):
    def upstream(request):
        raise AssertionError("Must not access sources")
    with client(upstream, monkeypatch) as api:
        assert api.get("/api/recommendations/books/me").status_code == 401
        assert api.get("/api/recommendations/books/me?limit=21", headers=headers()).status_code == 422


@pytest.mark.parametrize("status", [404, 500, 503, 401])
def test_profile_failures_not_presented_as_empty_lists(monkeypatch, status):
    with client(lambda request: httpx.Response(status), monkeypatch) as api:
        response = api.get("/api/recommendations/books/me", headers=headers())
        assert response.status_code == (401 if status == 401 else 503)


def test_private_lists_are_fresh_and_isolated_between_users(monkeypatch):
    alice, bob = headers(), headers()
    current = ["OL1W"]
    def upstream(request):
        if request.url.path.endswith("book-seeds"):
            refs = current if request.headers["Authorization"] == alice["Authorization"] else ["OL3W"]
            return httpx.Response(200, json={"bookIds": refs})
        if request.url.path.endswith("recommendation-candidates"):
            return httpx.Response(200, json=[book("OL1W"), book("OL2W"), book("OL3W")])
        return httpx.Response(200, json=book(request.url.path.rsplit("/", 1)[1]))
    with client(upstream, monkeypatch) as api:
        assert "OL1W" not in [b["externalId"] for b in api.get("/api/recommendations/books/me", headers=alice).json()["items"]]
        current.append("OL2W")
        second = api.get("/api/recommendations/books/me", headers=alice).json()
        assert second["seed_count"] == 2
        assert all(b["externalId"] not in current for b in second["items"])
        other = api.get("/api/recommendations/books/me", headers=bob).json()
        assert other["seed_count"] == 1
        assert "OL3W" not in [b["externalId"] for b in other["items"]]


def test_large_list_uses_bounded_metadata_requests_and_excludes_all_saved(monkeypatch):
    details = []
    saved = [f"OL{i}W" for i in range(1, 201)]
    def upstream(request):
        if request.url.path.endswith("book-seeds"):
            return httpx.Response(200, json={"bookIds": saved})
        if request.url.path.endswith("recommendation-candidates"):
            return httpx.Response(200, json=[book("OL199W"), book("OL999W")])
        oid = request.url.path.rsplit("/", 1)[1]
        details.append(oid)
        return httpx.Response(200, json=book(oid))
    with client(upstream, monkeypatch) as api:
        body = api.get("/api/recommendations/books/me", headers=headers()).json()
        assert body["seed_count"] == 200 and len(details) == 3
        assert [b["externalId"] for b in body["items"]] == ["OL999W"]


def test_home_uses_trained_neighbors_when_list_has_verified_mapping(tmp_path, monkeypatch):
    import pandas as pd
    from recommender.artifacts import save
    from recommender.model import fit
    catalog = pd.DataFrame(dict(item_id=["a", "b", "c"], title=["Ocean one", "Ocean two", "Poetry"], authors=["Ada"]*3, tags=[""]*3))
    ratings = pd.DataFrame([("1", "a", 5), ("1", "b", 5), ("2", "a", 5), ("2", "b", 5), ("2", "c", 4)], columns=["user_id", "item_id", "rating"])
    model = fit(catalog, ratings, include_nmf=False)
    model.external_ids = {"a": "OL1W", "b": "OL2W", "c": "OL3W"}
    path = tmp_path/"model"
    save(model, path, dict(version="fixture-knn", domain="books", namespace="goodbooks", algorithm="knn"))
    def upstream(request):
        if request.url.path.endswith("book-seeds"):
            return httpx.Response(200, json={"bookIds": ["OL1W"]})
        if request.url.path.endswith("recommendation-candidates"):
            return httpx.Response(200, json=[book("OL4W")])
        return httpx.Response(200, json=book(request.url.path.rsplit("/", 1)[1]))
    with TestClient(create_app(path, base64.b64encode(KEY).decode(), upstream_transport=httpx.MockTransport(upstream))) as api:
        response = api.get("/api/recommendations/books/me", headers=headers())
        assert response.status_code == 200
        body = response.json()
        assert body["mode"] == "hybrid"
        assert body["model_version"] == "fixture-knn"
        assert body["items"][0]["externalId"] == "OL2W"
        assert all(row["externalId"] != "OL1W" for row in body["items"])
