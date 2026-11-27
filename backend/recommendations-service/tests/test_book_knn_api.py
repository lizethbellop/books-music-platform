import base64
import secrets
import time
from uuid import uuid4

import httpx
import jwt
import pytest
from fastapi.testclient import TestClient

from recommender.api import create_app
from recommender.service import book_tastes


def api_client(monkeypatch):
    monkeypatch.delenv("MODEL_PATH", raising=False)
    key = secrets.token_bytes(64)
    claims = dict(sub=str(uuid4()), exp=time.time()+60, iss="musa-auth", aud="musa-api")
    headers = {"Authorization": "Bearer " + jwt.encode(claims, key, algorithm="HS512")}
    app = create_app(secret=base64.b64encode(key).decode())
    return TestClient(app), headers


def test_knn_authentication_and_limit_before_fetching_tastes(monkeypatch):
    def unexpected(*args, **kwargs):
        raise AssertionError("No debe consultar datos sin autorización válida")
    monkeypatch.setattr("recommender.api.recommend_books_for_user", unexpected)
    client, headers = api_client(monkeypatch)
    with client:
        assert client.get("/api/recommendations/books/knn/me").status_code == 401
        assert client.get("/api/recommendations/books/knn/me?limit=21", headers=headers).status_code == 422


def test_knn_passes_token_and_disables_personal_response_caching(monkeypatch):
    seen = []
    def recommend(token, limit):
        seen.append((token, limit))
        return {"mode": "knn_content", "items": []}
    monkeypatch.setattr("recommender.api.recommend_books_for_user", recommend)
    client, headers = api_client(monkeypatch)
    with client:
        response = client.get("/api/recommendations/books/knn/me?limit=5", headers=headers)
    assert response.status_code == 200
    assert seen == [(headers["Authorization"].removeprefix("Bearer "), 5)]
    assert response.headers["Cache-Control"] == "no-store"
    assert "Authorization" in response.headers["Vary"]


@pytest.mark.parametrize("status,expected", [(401,401), (403,403), (500,503)])
def test_upstream_failure_is_not_an_empty_profile(monkeypatch, status, expected):
    def recommend(*args):
        request = httpx.Request("GET", "http://books/books/recommendation-signals/me")
        httpx.Response(status, request=request).raise_for_status()
    monkeypatch.setattr("recommender.api.recommend_books_for_user", recommend)
    client, headers = api_client(monkeypatch)
    with client:
        response = client.get("/api/recommendations/books/knn/me", headers=headers)
    assert response.status_code == expected
    assert "items" not in response.json()


def test_tastes_propagates_token_and_keeps_accounts_separate(monkeypatch):
    real_client = httpx.Client
    calls = []
    def handler(request):
        auth = request.headers["Authorization"]
        calls.append((request.url.path, auth))
        assert "userId" not in request.url.params
        oid = "OL1W" if auth == "Bearer alice" else "OL2W"
        if request.url.path.endswith("book-seeds"):
            return httpx.Response(200, json={"bookIds": [oid]})
        return httpx.Response(200, json={"ratings": [], "libraryIds": []})
    monkeypatch.setenv("PROFILE_SERVICE_URL", "http://profile")
    monkeypatch.setenv("BOOKS_SERVICE_URL", "http://books")
    monkeypatch.setattr(book_tastes.httpx, "Client", lambda **kwargs: real_client(transport=httpx.MockTransport(handler), **kwargs))
    assert book_tastes.get_book_tastes("alice").seeds == {"OL1W": 1}
    assert book_tastes.get_book_tastes("bob").seeds == {"OL2W": 1}
    assert len(calls) == 4


def test_similar_auth_validates_id_limit_and_does_not_read_tastes(monkeypatch):
    calls = []
    def recommend(oid, limit, token=None):
        calls.append((oid, limit))
        return {"seed_count": 1, "mode": "knn_content", "items": []}
    monkeypatch.setattr("recommender.api.recommend_similar_books", recommend)
    monkeypatch.setattr("recommender.api.recommend_books_for_user", lambda *args: pytest.fail("No personal tastes"))
    client, headers = api_client(monkeypatch)
    with client:
        assert client.get("/api/recommendations/books/OL1W/similar").status_code == 401
        assert client.get("/api/recommendations/books/invalid/similar", headers=headers).status_code == 422
        assert client.get("/api/recommendations/books/OL1W/similar?limit=21", headers=headers).status_code == 422
        result = client.get("/api/recommendations/books/OL1W/similar?limit=4", headers=headers)
    assert result.status_code == 200
    assert calls == [("OL1W", 4)]
    assert result.headers["Cache-Control"] == "no-store"


def test_similar_uses_saved_features_and_excludes_reference(monkeypatch):
    from recommender.service import book_recommendations_service as service
    from test_book_knn_tastes import book
    monkeypatch.setenv("GROQ_MODEL", "test-model")
    monkeypatch.setattr(service, "list_catalog", lambda *args: [
        book("OL1W", themes=["relationships"], genres=["romance"]),
        book("OL2W", themes=["relationships"], genres=["romance"]),
        book("OL3W", themes=["murder"], genres=["mystery"]),
    ])
    monkeypatch.setattr(service, "get_book_tastes", lambda *args: pytest.fail("No personal tastes"))
    result = service.recommend_similar_books("/works/OL1W", 4)
    assert result["reference_book_id"] == "OL1W"
    assert [item["externalId"] for item in result["items"]] == ["OL2W"]
    assert result["items"][0]["sourceBookId"] == "OL1W"
    missing = service.recommend_similar_books("OL999W")
    assert missing["mode"] == "reference_not_prepared"
    assert missing["items"] == []


def test_similar_database_failure_is_not_disguised_as_empty(monkeypatch):
    import psycopg
    def fail(*args):
        raise psycopg.OperationalError("private database details")
    monkeypatch.setattr("recommender.api.recommend_similar_books", fail)
    client, headers = api_client(monkeypatch)
    with client:
        response = client.get("/api/recommendations/books/OL1W/similar", headers=headers)
    assert response.status_code == 503
    assert "private" not in response.text
