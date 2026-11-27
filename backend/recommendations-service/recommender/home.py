"""Home book recommendations from the authenticated user's lists.

Online retrieval uses musa's Books API; fixed feature hashing only transforms
metadata. No model is fit in a request and no cross-catalog identity is guessed.
"""
import asyncio
from collections import OrderedDict, Counter
import hashlib
import time

import httpx
import numpy as np
from fastapi import HTTPException
from sklearn.feature_extraction.text import HashingVectorizer

from .openlibrary import work_id


POLICY_VERSION = "home-books-v1"
# Stateless transformation: same representation for saved and candidate works.
FEATURES = HashingVectorizer(n_features=2**18, alternate_sign=False,
                            strip_accents="unicode", ngram_range=(1, 2), norm="l2")


def _strings(value):
    return [s.strip()[:160] for s in value if isinstance(s, str) and s.strip()] if isinstance(value, list) else []


def _book(value, expected_id=None):
    if not isinstance(value, dict):
        return None
    oid = work_id(value.get("externalId"))
    title = value.get("title")
    if not oid or (expected_id and oid != expected_id) or not isinstance(title, str) or not title.strip():
        return None
    return dict(externalId=oid, title=title.strip()[:500], authors=_strings(value.get("authors")),
                subjects=_strings(value.get("subjects"))[:40], coverUrl=value.get("coverUrl"),
                firstPublishYear=value.get("firstPublishYear"))


def _text(book):
    return " ".join([book["title"], *book["authors"], *book["authors"],
                     *book["subjects"], *book["subjects"]])


def _quoted(value):
    return '"' + value[:100].replace('\\', '\\\\').replace('"', '\\"') + '"'


def _query(seeds):
    subjects = Counter(s for b in seeds for s in set(b["subjects"]))
    authors = Counter(s for b in seeds for s in set(b["authors"]))
    terms = ["subject:"+_quoted(s) for s, _ in sorted(subjects.items(), key=lambda v: (-v[1], v[0]))[:3]]
    terms += ["author:"+_quoted(s) for s, _ in sorted(authors.items(), key=lambda v: (-v[1], v[0]))[:2]]
    if not terms:
        terms = ["title:"+_quoted(seeds[0]["title"])]
    return " OR ".join(terms)


class HomeRecommender:
    def __init__(self, client, model, metadata, profile_url, books_url):
        self.client, self.model, self.metadata = client, model, metadata
        self.profile_url, self.books_url = profile_url.rstrip("/"), books_url.rstrip("/")
        self.cache = OrderedDict()
        self.capacity, self.ttl = 256, 600
        self.requests = asyncio.Semaphore(3)

    async def _get(self, url, token, params=None, public=False, missing=None):
        key = (url, tuple(sorted((params or {}).items())))
        cached = self.cache.get(key) if public else None
        if cached and cached[0] > time.monotonic():
            self.cache.move_to_end(key)
            return cached[1]
        try:
            async with self.requests:
                response = await self.client.get(url, params=params,
                                                 headers={"Authorization": "Bearer "+token})
            if response.status_code in (401, 403):
                raise HTTPException(response.status_code, "Tu sesión no permite consultar este contenido")
            if response.status_code == 404 and missing is not None:
                return missing
            response.raise_for_status()
            value = response.json()
        except (httpx.HTTPError, ValueError):
            raise HTTPException(503, "No se pudieron consultar tus libros. Intenta nuevamente.")
        if public:
            self.cache[key] = (time.monotonic()+self.ttl, value)
            self.cache.move_to_end(key)
            while len(self.cache) > self.capacity:
                self.cache.popitem(last=False)
        return value

    async def _detail(self, oid, token):
        try:
            raw = await self._get(self.books_url+"/books/"+oid, token, public=True, missing={})
            return _book(raw, oid)
        except HTTPException as exc:
            if exc.status_code in (401, 403):
                raise
            return None

    def _neighbors(self, saved):
        if self.model is None or self.metadata.get("domain") != "books":
            return {}
        model = self.model
        indices = [model._index[i] for i, oid in model.external_ids.items() if oid in saved]
        if not indices:
            return {}
        similarities = np.asarray(model.neighbors[indices].mean(axis=0)).ravel()
        scores = {}
        for internal, oid in model.external_ids.items():
            score = float(similarities[model._index[internal]])
            if oid not in saved and score > 0:
                scores[oid] = max(scores.get(oid, 0), score)
        scale = max(scores.values(), default=1)
        return {oid: score/scale for oid, score in scores.items()}

    async def recommend(self, token, limit):
        # Never cached: ownership and list changes are checked on every Home refresh.
        references = await self._get(self.profile_url+"/api/profiles/me/lists/book-seeds", token)
        if not isinstance(references, dict) or not isinstance(references.get("bookIds"), list):
            raise HTTPException(503, "No se pudieron consultar tus listas")
        saved = {oid for v in references["bookIds"] if isinstance(v, str) and (oid := work_id(v))}
        if not saved:
            raw = await self._get(self.books_url+"/books/explore", token, public=True)
            if not isinstance(raw, list):
                raise HTTPException(503, "El catálogo no está disponible")
            unique = {}
            for value in raw:
                if book := _book(value):
                    unique.setdefault(book["externalId"], dict(book, reason="Una lectura para descubrir", score=0.0))
            return self._response(list(unique.values())[:limit], 0, "explore", False)

        # Uniform deterministic sample by ID hash; all saved IDs remain exclusions.
        sampled = sorted(saved, key=lambda v: hashlib.sha256(v.encode()).hexdigest())[:3]
        details = await asyncio.gather(*(self._detail(oid, token) for oid in sampled))
        seeds = [b for b in details if b]
        partial = len(seeds) < len(sampled)
        graph_scores = self._neighbors(saved)
        candidates = {}
        if seeds:
            try:
                raw = await self._get(self.books_url+"/books/recommendation-candidates", token,
                                      params={"q": _query(seeds)}, public=True)
                if not isinstance(raw, list):
                    raise HTTPException(503, "El catálogo no está disponible")
                for value in raw:
                    if (book := _book(value)) and book["externalId"] not in saved:
                        candidates[book["externalId"]] = book
            except HTTPException as exc:
                if exc.status_code in (401, 403):
                    raise
                partial = True
        # At most three extra metadata requests; no 10k-book network fan-out.
        extra_ids = [oid for oid in sorted(graph_scores, key=lambda oid: (-graph_scores[oid], oid))
                     if oid not in candidates][:3]
        extra = await asyncio.gather(*(self._detail(oid, token) for oid in extra_ids))
        for book in extra:
            if book:
                candidates[book["externalId"]] = book
            else:
                partial = True
        if not seeds and not candidates:
            raise HTTPException(503, "No se pudieron cargar los libros de tus listas")
        if not candidates:
            if partial:
                raise HTTPException(503, "No se pudieron cargar las recomendaciones")
            return self._response([], len(saved), "content", False)

        books = [candidates[k] for k in sorted(candidates)]
        if seeds:
            similarity = (FEATURES.transform([_text(b) for b in books]) @
                          FEATURES.transform([_text(b) for b in seeds]).T).toarray()
        else:
            similarity = np.zeros((len(books), 1))
        ranked = []
        for i, book in enumerate(books):
            seed_pos = int(np.argmax(similarity[i]))
            content = float(similarity[i, seed_pos])
            collaborative = graph_scores.get(book["externalId"], 0)
            score = .7*content + .3*collaborative if graph_scores else content
            if score <= 0:
                continue
            reason = ("Porque guardaste «"+seeds[seed_pos]["title"]+"»"
                      if content > 0 and seeds else "Lecturas afines a tus listas")
            ranked.append(dict(book, score=score, reason=reason))
        ranked.sort(key=lambda b: (-b["score"], b["externalId"]))
        # Diversity soft cap; fill remaining places afterwards if few authors exist.
        selected, deferred, authors = [], [], Counter()
        for book in ranked:
            author = book["authors"][0].casefold() if book["authors"] else book["externalId"]
            if authors[author] >= 2:
                deferred.append(book)
            else:
                selected.append(book)
                authors[author] += 1
        result = (selected+deferred)[:limit]
        return self._response(result, len(saved), "hybrid" if graph_scores else "content", partial)

    def _response(self, items, seed_count, mode, partial):
        return dict(policy_version=POLICY_VERSION, model_version=self.metadata["version"],
                    namespace="openlibrary-work", seed_count=seed_count, mode=mode,
                    partial=partial, items=items)
