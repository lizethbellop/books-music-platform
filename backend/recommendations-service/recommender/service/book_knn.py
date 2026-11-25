import math
import unicodedata
from collections import Counter

from sklearn.feature_extraction import DictVectorizer
from sklearn.neighbors import NearestNeighbors
from sklearn.feature_extraction.text import TfidfTransformer

from ..feature_extractor import BookFeatures
from ..features import normalize_label
from .book_tastes import BookTastes


# Pesos iniciales de grupos, no resultados de una optimización.
GROUP_WEIGHTS = {
    "genres": 2.0,
    "themes": 3.0,
    "settings": 1.5,
    "tropes": 2.0,
    "audience": 0.5,
    "formats": 0.5,
}
GENERIC = {'fiction', 'nonfiction', 'non fiction', 'literature', 'novel', 'books', 'book'}
MEANING_GROUPS = {"genres", "themes", "settings", "tropes"}


def feature_label(group: str, value: str) -> str:
    label = normalize_label(value)
    # OpenLibrary/LLM metadata can spell the same label with Unicode dashes.
    label = " ".join("".join(
        " " if unicodedata.category(char) == "Pd" or char == "_" else char
        for char in label
    ).split())
    if group == 'genres' and label == 'non fiction':
        return 'nonfiction'
    # In the genres group, the suffix in "romance fiction" is redundant.
    if group == "genres" and label.endswith(" fiction"):
        label = label.removesuffix(" fiction").strip()
    return label


def feature_vector(features: dict) -> dict[str, float]:
    validated = BookFeatures.model_validate(features)
    vector = {}
    for group, values in validated.model_dump().items():
        labels = {feature_label(group, value) for value in values if value.strip()}
        if not labels:
            continue
        # Cada grupo aporta el peso acordado, independientemente de cuántas
        # etiquetas contenga. La raíz se usa porque el coseno multiplica valores.
        value = math.sqrt(GROUP_WEIGHTS[group] / len(labels))
        for label in labels:
            vector[f"{group}:{label}"] = value * (0.25 if label in GENERIC else 1)
    return vector


def recommend_from_catalog(
    catalog: list[dict], tastes: BookTastes, limit: int = 12,
) -> dict:
    if not 1 <= limit <= 20:
        raise ValueError("limit debe estar entre 1 y 20")

    records, vectors = [], []
    for book in catalog:
        vector = feature_vector(book["features"])
        if vector:
            records.append(book)
            vectors.append(vector)

    index = {book["externalId"]: i for i, book in enumerate(records)}
    seed_ids = sorted(set(tastes.seeds) & set(index))
    missing = sorted(set(tastes.seeds) - set(seed_ids))

    base = {
        "policy_version": "book-features-knn-v3",
        "catalog_size": len(records),
        "seed_count": len(tastes.seeds),
        "usable_seed_count": len(seed_ids),
        "missing_seed_ids": missing,
        "partial": bool(missing),
    }

    if not records:
        return dict(base, mode="catalog_not_ready", items=[])

    if not tastes.seeds:
        # Orden estable, no popularidad ni personalización inventadas.
        items = [
            card(book, 0.0, "Una lectura para descubrir", None, [])
            for book in records if book["externalId"] not in tastes.exclude
        ]
        return dict(base, mode="explore", items=diversify(items, limit))

    if not seed_ids:
        return dict(base, mode="profile_not_prepared", items=[])

    vectorizer = DictVectorizer(sparse=True)
    matrix = vectorizer.fit_transform(vectors)
    matrix = TfidfTransformer(norm=None).fit_transform(matrix)
    candidate_positions = [
        i for i, book in enumerate(records)
        if book["externalId"] not in tastes.exclude
        and book["externalId"] not in tastes.seeds
    ]
    if not candidate_positions:
        return dict(base, mode="knn_content", items=[])

    neighbors = NearestNeighbors(metric="cosine", algorithm="brute")
    neighbors.fit(matrix[candidate_positions])
    # Para el catálogo inicial pequeño consultamos todos los candidatos y luego
    # filtramos. Así no perdemos vecinos válidos por exclusiones o empates.
    # No se materializa una matriz completa catálogo x catálogo.
    distances, positions = neighbors.kneighbors(
        matrix[[index[oid] for oid in seed_ids]],
        n_neighbors=len(candidate_positions),
    )

    ranked = {}
    max_weight = max(tastes.seeds[oid] for oid in seed_ids)
    for row, seed_id in enumerate(seed_ids):
        seed_position = index[seed_id]
        for distance, position in zip(distances[row], positions[row]):
            candidate_position = candidate_positions[int(position)]
            common = sorted(set(vectors[seed_position]) & set(vectors[candidate_position]))
            meaningful = [
                key for key in common if key.split(":", 1)[0] in MEANING_GROUPS and key.split(":", 1)[1] not in GENERIC
            ]
            # Compartir solo público o formato no basta para recomendar.
            if not meaningful:
                continue
            similarity = max(0.0, min(1.0, 1.0 - float(distance)))
            score = similarity * tastes.seeds[seed_id] / max_weight
            if score <= 0:
                continue
            book = records[candidate_position]
            oid = book["externalId"]
            if oid in ranked and ranked[oid]["score"] >= score:
                continue
            labels = [key.split(":", 1)[1] for key in meaningful[:3]]
            seed_title = records[seed_position]["metadata"]["title"]
            reason = f"Comparte {', '.join(labels)} con «{seed_title}»"
            ranked[oid] = card(book, score, reason, seed_id, meaningful)

    items = sorted(ranked.values(), key=lambda b: (-b["score"], b["externalId"]))
    return dict(base, mode="knn_content", items=diversify(items, limit))


def card(book, score, reason, seed_id, matches):
    metadata = book["metadata"]
    return {
        "externalId": book["externalId"],
        "title": metadata["title"],
        "authors": metadata.get("authors", []),
        "coverUrl": metadata.get("coverUrl"),
        "firstPublishDate": metadata.get("firstPublishDate"),
        "score": round(score, 6),
        "reason": reason,
        "sourceBookId": seed_id,
        "matchedFeatures": matches,
    }


def diversify(items, limit):
    selected, deferred, counts = [], [], Counter()
    for item in items:
        authors = item.get("authors") or []
        author = normalize_label(authors[0]) if authors else item["externalId"]
        if counts[author] >= 2:
            deferred.append(item)
        else:
            selected.append(item)
            counts[author] += 1
    return (selected + deferred)[:limit]
