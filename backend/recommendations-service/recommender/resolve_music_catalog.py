"""Resuelve correspondencias por enlaces explícitos o ISRC exacto, no por parecido."""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import time

from dotenv import dotenv_values, load_dotenv
import httpx

from .provider_budget import provider_request
from .music_models import MUSIC_FEATURE_VERSION, MusicRef
from .music_features_repository import get_music_features, save_music_features
from .musicbrainz_client import ENTITIES, USER_AGENT, parse_musicbrainz


class CatalogResolver:
    def __init__(self, spotify_env=None, cache_dir="artifacts/musicbrainz-cache"):
        self.client = httpx.Client(timeout=25)
        self.cache = Path(cache_dir)
        self.cache.mkdir(parents=True, exist_ok=True)
        self.last_request = 0.0
        self.spotify_env = dotenv_values(spotify_env) if spotify_env else {}
        self.spotify_token = None

    def close(self):
        self.client.close()

    def mb(self, path, **params):
        params["fmt"] = "json"
        digest = hashlib.sha256(json.dumps([path, params], sort_keys=True).encode()).hexdigest()
        file = self.cache / (digest + ".json")
        if file.exists() and time.time()-file.stat().st_mtime < 7*86400:
            return json.loads(file.read_text())
        wait = 1.1 - (time.monotonic() - self.last_request)
        if wait > 0:
            time.sleep(wait)
        self.last_request = time.monotonic()
        response = provider_request(self.client, 'musicbrainz', 'GET',
            'https://musicbrainz.org/ws/2/' + path, params=params, headers={'User-Agent':USER_AGENT})
        result = response.json()
        file.write_text(json.dumps(result, ensure_ascii=False))
        return result

    def spotify_isrc(self, oid):
        # Se usa el identificador ISRC para verificar identidad, nunca como rasgo
        # musical ni se envía el contenido Spotify a Groq/KNN.
        if self.spotify_token is None:
            key = os.environ.get("SPOTIFY_CLIENT_ID") or self.spotify_env.get("SPOTIFY_CLIENT_ID")
            secret = os.environ.get("SPOTIFY_CLIENT_SECRET") or self.spotify_env.get("SPOTIFY_CLIENT_SECRET")
            if not key or not secret:
                return None
            response = provider_request(self.client, "spotify", "POST", "https://accounts.spotify.com/api/token",
                                        auth=(key, secret), data={"grant_type": "client_credentials"})
            response.raise_for_status()
            self.spotify_token = response.json()["access_token"]
        response = provider_request(self.client, "spotify", "GET", "https://api.spotify.com/v1/tracks/" + oid,
                                   headers={"Authorization": "Bearer " + self.spotify_token})
        response.raise_for_status()
        return response.json().get("external_ids", {}).get("isrc")

    def resolve(self, ref):
        kind = {"ARTIST": "artist", "ALBUM": "album", "SONG": "track"}[ref.contentType]
        resource = f"https://open.spotify.com/{kind}/{ref.externalId}"
        inc = {"ARTIST": "artist-rels", "ALBUM": "release-rels+release-group-rels",
               "SONG": "recording-rels"}[ref.contentType]
        payload = self.mb("url", resource=resource, inc=inc)
        ids = set()
        if payload:
            if payload.get("resource") != resource:
                raise ValueError("La URL de origen no coincide")
            for relation in payload.get("relations", []):
                entity = relation.get(ENTITIES[ref.contentType])
                if isinstance(entity, dict) and entity.get("id"):
                    ids.add(entity["id"])
                if ref.contentType == "ALBUM" and isinstance(relation.get("release"), dict):
                    release = self.mb("release/" + relation["release"]["id"], inc="release-groups")
                    if release and release.get("release-group", {}).get("id"):
                        ids.add(release["release-group"]["id"])
        evidence = resource
        if not ids and ref.contentType == "SONG":
            isrc = self.spotify_isrc(ref.externalId)
            if isrc:
                result = self.mb("recording", query="isrc:" + isrc, limit=10)
                recordings = (result or {}).get("recordings", [])
                if (result or {}).get("count", 0) == 1 and len(recordings) == 1:
                    record = self.mb("recording/" + recordings[0]["id"], inc="isrcs")
                    if record and isrc.upper() in {value.upper() for value in record.get("isrcs", [])}:
                        ids.add(record["id"])
                        evidence = "ISRC:" + isrc.upper()
        if len(ids) != 1:
            return None, "sin correspondencia única"
        return next(iter(ids)), evidence

    def features(self, ref, mbid):
        inc = "tags+genres" if ref.contentType == "ARTIST" else "tags+genres+artist-credits"
        payload = self.mb(ENTITIES[ref.contentType] + "/" + mbid, inc=inc)
        if payload is None:
            raise ValueError("La entidad desapareció de MusicBrainz")
        return parse_musicbrainz(ref.contentType, mbid, payload)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="CSV: content_type,spotify_id")
    parser.add_argument("--output", default="examples/music_catalog_verified.csv")
    parser.add_argument("--spotify-env", help="Archivo local del operador para verificar ISRC")
    args = parser.parse_args()
    load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=False)
    with open(args.input, encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) > 50:
        parser.error("Preparar como máximo 50 referencias por ejecución")
    resolver = CatalogResolver(args.spotify_env)
    verified, skipped, used = [], [], set()
    previous_evidence = {}
    previous = Path(args.output)
    if previous.exists():
        with previous.open(encoding="utf-8", newline="") as stream:
            for row in csv.DictReader(stream):
                previous_evidence[(row["content_type"], row["spotify_id"])] = row.get("evidence", "catalogo-verificado")
    try:
        for row in rows:
            ref = MusicRef(contentType=row["content_type"], externalId=row["spotify_id"])
            cached = get_music_features(ref.contentType, ref.externalId)
            if cached and cached["feature_version"] == MUSIC_FEATURE_VERSION:
                mbid, evidence = cached["musicbrainz_id"], previous_evidence.get((ref.contentType, ref.externalId), "catalogo-verificado")
                if (ref.contentType, mbid) in used:
                    continue
            else:
                try:
                    mbid, evidence = resolver.resolve(ref)
                    if mbid is None or (ref.contentType, mbid) in used:
                        skipped.append({"content_type": ref.contentType, "spotify_id": ref.externalId,
                                        "reason": evidence if mbid is None else "misma obra/otra edición"})
                        print(f"Pendiente: {ref.key} ({skipped[-1]['reason']})", flush=True)
                        continue
                    features, metadata, source_hash = resolver.features(ref, mbid)
                    save_music_features(ref.contentType, ref.externalId, mbid,
                                        features.model_dump(), metadata, source_hash)
                except (httpx.HTTPError, ValueError) as error:
                    status = error.response.status_code if isinstance(error, httpx.HTTPStatusError) else type(error).__name__
                    skipped.append({"content_type": ref.contentType, "spotify_id": ref.externalId,
                                    "reason": f"consulta pendiente: {status}"})
                    print(f"Pendiente: {ref.key} ({status})", flush=True)
                    continue
            used.add((ref.contentType, mbid))
            verified.append({"content_type": ref.contentType, "spotify_id": ref.externalId,
                             "musicbrainz_id": mbid, "verified": "true", "evidence": evidence})
            print(f"Preparado: {ref.key}", flush=True)
    finally:
        resolver.close()
        path = Path(args.output)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.DictWriter(stream, fieldnames=["content_type", "spotify_id", "musicbrainz_id", "verified", "evidence"])
            writer.writeheader(); writer.writerows(verified)
        path.with_suffix(".pending.json").write_text(json.dumps(skipped, ensure_ascii=False, indent=2))
        print(f"Verificados: {len(verified)}. Pendientes: {len(skipped)}.", flush=True)


if __name__ == "__main__":
    main()
