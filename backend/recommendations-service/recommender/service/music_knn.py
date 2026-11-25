import math
import re
import unicodedata
from collections import Counter

from sklearn.feature_extraction import DictVectorizer
from sklearn.neighbors import NearestNeighbors
from sklearn.feature_extraction.text import TfidfTransformer

from ..features import normalize_label
from ..music_models import MusicFeatures
from .music_tastes import MusicTastes

WEIGHTS = {"genres": 3.0, "tags": 2.0, "artists": 0.5}


GENERIC = {'pop','rock','electronic','dance','music'}
NOISE = re.compile(r'billboard|charts?|jahrescharts|wochen|^dr[0-9]+$|my top|well.known|self.titled|^[0-9]{4}s?$')


def clean_label(label):
    label=normalize_label(label)
    label=' '.join(''.join(' ' if unicodedata.category(char)=='Pd' else char for char in label).split())
    if NOISE.search(label):
        return ''
    return {'synthpop':'synth pop','rnb':'r&b','rhythm and blues':'r&b'}.get(label,label)


def music_vector(features: dict) -> dict:
    validated = MusicFeatures.model_validate(features)
    result = {}
    genres={clean_label(label) for label in validated.genres} - {''}
    for group, values in validated.model_dump().items():
        labels = ({label for label in values if label.strip()} if group=='artists'
                  else {clean_label(label) for label in values} - {''})
        if group=='tags':
            labels -= genres  # A genre repeated as a tag is a single signal.
        if labels:
            value=math.sqrt(WEIGHTS[group]/len(labels))
            result.update({f'{group}:{label}':value*(0.25 if label in GENERIC else 1) for label in labels})
    return result


def music_card(item, score, reason, source_key=None, matches=None):
    metadata = item["metadata"]
    kind = {"SONG": "track", "ALBUM": "album", "ARTIST": "artist"}[item["contentType"]]
    return {
        "spotifyId": item["externalId"], "contentType": item["contentType"],
        "name": metadata["name"], "artistNames": metadata.get("artistNames", []),
        "releaseDate": metadata.get("releaseDate"),
        "artistIds": item["features"].get("artists", []),
        "spotifyUrl": f"https://open.spotify.com/{kind}/{item['externalId']}",
        "source": "musicbrainz", "sourceUrl": metadata["sourceUrl"],
        "score": round(score, 6), "reason": reason,
        "sourceItemKey": source_key, "matchedFeatures": matches or [],
    }


def diversify_music(items, limit):
    chosen, counts, seen = [], Counter(), set()
    for item in items:
        artists=item.get('artistIds') or [normalize_label(name) for name in item.get('artistNames',[])] or [item['spotifyId']]
        identity=(tuple(sorted(artists)),normalize_label(item['name']))
        if identity in seen or any(counts[artist]>=2 for artist in artists):
            continue
        seen.add(identity)
        chosen.append(item)
        counts.update(artists)
        if len(chosen)>=limit:
            break
    return chosen


def recommend_music(catalog: list[dict], tastes: MusicTastes, content_type: str, limit: int = 12):
    if content_type not in {"SONG", "ALBUM", "ARTIST"} or not 1 <= limit <= 20:
        raise ValueError("Tipo o límite musical inválido")
    records, vectors = [], []
    for item in catalog:
        vector = music_vector(item["features"])
        if vector:
            records.append(item)
            vectors.append(vector)
    index = {item["key"]: i for i, item in enumerate(records)}
    seed_keys = sorted(set(tastes.seeds) & set(index))
    missing = sorted(set(tastes.seeds) - set(seed_keys))
    positions = [i for i, item in enumerate(records)
                 if item["contentType"] == content_type
                 and item["key"] not in tastes.exclude and item["key"] not in tastes.seeds]
    base = {"policy_version": "music-features-knn-v2", "contentType": content_type,
            "catalog_size": sum(item["contentType"] == content_type for item in records),
            "seed_count": len(tastes.seeds), "usable_seed_count": len(seed_keys),
            "partial": bool(missing), "missing_seed_keys": missing}
    if base["catalog_size"] == 0:
        return dict(base, mode="catalog_not_ready", items=[])
    if not tastes.seeds:
        items = [music_card(records[i], 0, "Una opción para descubrir") for i in positions]
        return dict(base, mode="explore", items=diversify_music(items, limit))
    if not seed_keys:
        return dict(base, mode="profile_not_prepared", items=[])
    if not positions:
        return dict(base, mode="knn_content", items=[])
    matrix = DictVectorizer(sparse=True).fit_transform(vectors)
    matrix = TfidfTransformer(norm=None).fit_transform(matrix)
    knn = NearestNeighbors(metric="cosine", algorithm="brute").fit(matrix[positions])
    distances, neighbors = knn.kneighbors(matrix[[index[key] for key in seed_keys]], n_neighbors=len(positions))
    ranked = {}
    max_weight = max(tastes.seeds[key] for key in seed_keys)
    for row, seed_key in enumerate(seed_keys):
        seed = records[index[seed_key]]
        for distance, position in zip(distances[row], neighbors[row]):
            i = positions[int(position)]
            common = sorted(set(vectors[index[seed_key]]) & set(vectors[i]))
            similarity = max(0.0, min(1.0, 1 - float(distance)))
            same_artist = any(key.startswith('artists:') for key in common)
            affinity = 0.8 * similarity + (0.2 if same_artist else 0.0)
            score = min(1.0, affinity) * tastes.seeds[seed_key] / max_weight
            meaningful=[key for key in common if key.startswith('artists:') or key.split(':',1)[1] not in GENERIC]
            if not meaningful or score <= 0:
                continue
            candidate = records[i]
            key = candidate["key"]
            if key in ranked and ranked[key]["score"] >= score:
                continue
            labels = sorted({c.split(":", 1)[1] for c in meaningful if not c.startswith("artists:")})[:3]
            if labels:
                reason = f"Comparte {'artista y ' if same_artist else ''}{', '.join(labels)} con «{seed['metadata']['name']}»"
            else:
                reason = f"Comparte artista con «{seed['metadata']['name']}»"
            ranked[key] = music_card(candidate, score, reason, seed_key, common)
    items = sorted(ranked.values(), key=lambda item: (-item["score"], item["spotifyId"]))
    return dict(base, mode="knn_content", items=diversify_music(items, limit))
