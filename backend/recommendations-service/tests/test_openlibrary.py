import base64
import gzip
import json
import time
from uuid import uuid4

import jwt
import pandas as pd
import pytest
from fastapi.testclient import TestClient

from recommender.api import create_app
from recommender.artifacts import save
from recommender.model import fit
from recommender.openlibrary import isbn13, work_id, collect_dump, collect_search, resolve, load_crosswalk


@pytest.mark.parametrize("raw,expected", [
    ("439023483", "9780439023481"),
    ("0439023483", "9780439023481"),
    ("978-0-439-02348-1", "9780439023481"),
    ("9.780439023481e12", "9780439023481"),
    ("9.78043902348e12", None),  # rounded Goodbooks ISBN-13 fails checksum
    ("1234567890", None), ("", None), ("NaN", None),
])
def test_isbn_validation(raw, expected):
    assert isbn13(raw) == expected


def test_dump_ambiguity_and_crosswalk(tmp_path):
    isbn = "9780439023481"
    records = [dict(key="/books/OL1M", isbn_13=[isbn], works=[dict(key="/works/OL1W")]),
               dict(key="/books/OL2M", isbn_10=["0439023483"], works=[dict(key="/works/OL2W")])]
    path = tmp_path/"editions.txt.gz"
    with gzip.open(path, "wt") as stream:
        for row in records:
            stream.write("/type/edition\t"+row["key"]+"\t1\t2026-10-28\t"+json.dumps(row)+"\n")
    candidates = collect_dump(path, {isbn})
    books = pd.DataFrame([dict(id="1", isbn="439023483", isbn13="")])
    result = resolve(books, candidates, "fixture")
    assert result.iloc[0].status == "ambiguous"
    assert result.iloc[0].work_id == ""
    result = resolve(books, {isbn: {"OL1W": "/books/OL1M"}}, "fixture")
    result.to_csv(tmp_path/"map.csv", index=False)
    assert load_crosswalk(tmp_path/"map.csv", ["1"]) == {"1": "OL1W"}
    assert work_id("/works/OL1W") == "OL1W"
    assert work_id("OL1M") is None
    assert work_id("/works/../authors/OL1A") is None


def test_search_requires_complete_exact_isbn_evidence():
    isbn = "9780439023481"
    payload = dict(start=0, numFound=1, docs=[dict(key="/works/OL1W", title="Title", isbn=[isbn])])
    assert set(collect_search(payload, {isbn})[isbn]) == {"OL1W"}
    assert collect_search(payload, {"9780140328721"})["9780140328721"] == {}
    payload["numFound"] = 2
    with pytest.raises(ValueError, match="Incomplete"):
        collect_search(payload, {isbn})


def test_api_only_serves_verified_works_and_excludes_aliases(tmp_path):
    catalog = pd.DataFrame(dict(item_id=list("abcd"), title=["Space one", "Space edition", "Poetry", "Unknown title"], authors=["A"]*4, tags=[""]*4))
    ratings = pd.DataFrame([("1", "a", 5), ("1", "c", 4), ("2", "b", 5), ("2", "c", 5)], columns=["user_id", "item_id", "rating"])
    model = fit(catalog, ratings, include_nmf=False)
    model.external_ids = {"a": "OL1W", "b": "OL1W", "c": "OL2W"}
    path = tmp_path/"v2"
    save(model, path, dict(version="v2", domain="books", namespace="goodbooks", serving_namespace="openlibrary-work", algorithm="hybrid"))
    key = b"s"*64
    token = jwt.encode(dict(sub=str(uuid4()), exp=time.time()+60, iss="musa-auth", aud="musa-api"), key, algorithm="HS512")
    with TestClient(create_app(path, base64.b64encode(key).decode())) as client:
        headers = {"Authorization": "Bearer "+token}
        result = client.post("/api/recommendations/books", headers=headers, json={"seeds": ["/works/OL1W"]})
        assert result.status_code == 200
        body = result.json()
        assert body["namespace"] == "openlibrary-work"
        assert [r["externalId"] for r in body["items"]] == ["OL2W"]
        assert body["items"][0]["item_id"] == "OL2W"
        fallback = client.post("/api/recommendations/books", headers=headers, json={"seeds": ["OL999W"]}).json()
        assert fallback["unmapped_seeds"] == ["OL999W"]
        assert len(fallback["items"]) == 2  # aliases are deduplicated
        assert all(r["reason"] == "popularity" for r in fallback["items"])
        assert client.post("/api/recommendations/books", headers=headers, json={"seeds": ["1"]}).status_code == 422
        assert client.post("/api/recommendations/books", headers=headers, json={"exclude": ["OL1W", "OL2W"]}).json()["items"] == []
