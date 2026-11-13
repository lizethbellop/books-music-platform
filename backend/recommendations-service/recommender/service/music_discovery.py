"""Public MusicBrainz discovery with identifier-verified Spotify mappings."""
import os
import re
import time
from urllib.parse import urlparse
from uuid import UUID

from ..music_models import MusicRef
from ..musicbrainz_client import ENTITIES, parse_musicbrainz
from ..provider_budget import provider_request
from ..resolve_music_catalog import CatalogResolver


class MappingUnavailable(ValueError):
    pass


def spotify_links(payload, content_type):
    kind = {'SONG':'track','ALBUM':'album','ARTIST':'artist'}[content_type]
    ids = set()
    for relation in payload.get('relations', []):
        target = relation.get('url', {}).get('resource', '')
        parsed = urlparse(target)
        parts = parsed.path.strip('/').split('/')
        if len(parts) == 3 and parts[0].startswith('intl-'):
            parts = parts[1:]
        if parsed.scheme == 'https' and parsed.hostname == 'open.spotify.com' and len(parts) == 2:
            if parts[0] == kind and re.fullmatch(r'[A-Za-z0-9]{22}', parts[1]):
                ids.add(parts[1])
    return ids


class MusicDiscovery(CatalogResolver):
    def spotify(self, path, params=None):
        if self.spotify_token is None or time.monotonic() >= getattr(self, 'token_deadline', 0):
            key = os.environ.get('SPOTIFY_CLIENT_ID') or self.spotify_env.get('SPOTIFY_CLIENT_ID')
            secret = os.environ.get('SPOTIFY_CLIENT_SECRET') or self.spotify_env.get('SPOTIFY_CLIENT_SECRET')
            if not key or not secret:
                return None
            result = provider_request(self.client, 'spotify', 'POST', 'https://accounts.spotify.com/api/token',
                auth=(key,secret),data={'grant_type':'client_credentials'}).json()
            self.spotify_token = result['access_token']
            self.token_deadline = time.monotonic() + max(0,int(result.get('expires_in',3600))-60)
        try:
            return provider_request(self.client,'spotify','GET','https://api.spotify.com/v1/'+path,
                params=params, headers={'Authorization':'Bearer '+self.spotify_token}).json()
        except Exception as error:
            if getattr(getattr(error,'response',None),'status_code',None) == 401:
                self.spotify_token = None
            raise

    def spotify_isrc(self, oid):
        result = self.spotify('tracks/'+oid)
        return (result or {}).get('external_ids',{}).get('isrc')

    def entity(self, kind, mbid):
        inc = 'tags+genres+url-rels'
        if kind != 'ARTIST':
            inc += '+artist-credits'
        if kind == 'SONG':
            inc += '+isrcs'
        result = self.mb(ENTITIES[kind]+'/'+str(UUID(mbid)), inc=inc)
        if not result:
            raise MappingUnavailable('musicbrainz_entity_unavailable')
        return result

    def from_mbid(self, kind, mbid):
        payload = self.entity(kind,mbid)
        ids = spotify_links(payload,kind)
        evidence = 'MusicBrainz URL relationship'
        if not ids and kind == 'ALBUM':
            releases = self.mb('release', **{'release-group':mbid,'status':'official','limit':3,'inc':'url-rels'})
            for release in (releases or {}).get('releases',[]):
                ids.update(spotify_links(release,kind))
        if not ids and kind == 'SONG':
            codes = sorted({value.upper() for value in payload.get('isrcs',[]) if re.fullmatch(r'[A-Za-z]{2}[A-Za-z0-9]{3}[0-9]{7}',value)})
            if codes:
                result = self.spotify('search',{'q':'isrc:'+codes[0],'type':'track','limit':5})
                for track in (result or {}).get('tracks',{}).get('items',[]):
                    # Only IDs and exact recording identifiers are read. Spotify
                    # titles, genres, images and descriptions never enter vectors.
                    if track.get('external_ids',{}).get('isrc','').upper() == codes[0]:
                        oid = track.get('id','')
                        if re.fullmatch(r'[A-Za-z0-9]{22}',oid):
                            ids.add(oid)
                evidence = 'ISRC:'+codes[0]
        if not ids:
            raise MappingUnavailable('no_verified_spotify_mapping')
        # Multiple links from the same verified MB entity are editions of that
        # entity. Keep one deterministic edition and alias user IDs when needed.
        oid = min(ids)
        features, metadata, source_hash = parse_musicbrainz(kind,mbid,payload)
        return oid,features,metadata,source_hash,evidence

    def discover(self, saved, known_artists=()):
        from .music_knn import clean_label, GENERIC
        candidates = []
        seen = set()
        def add(kind, identifier):
            try:
                identifier = str(UUID(identifier))
            except (ValueError,TypeError,AttributeError):
                return
            key = (kind,identifier)
            if key not in seen:
                seen.add(key)
                candidates.append(key)
        artists = saved['features'].get('artists',[])[:1]
        for artist in artists:
            add('ARTIST',artist)
        genres = [label.strip().casefold() for label in saved['features'].get('genres',[])
                  if clean_label(label) and clean_label(label) not in GENERIC][:2]
        if genres:
            query = ' OR '.join('tag:"'+label.replace('"','')+'"' for label in genres)
            result = self.mb('artist',query=query,limit=4)
            for item in (result or {}).get('artists',[]):
                if item.get('id') not in artists and item.get('id') not in known_artists:
                    artists.append(item['id'])
                    add('ARTIST',item['id'])
                    break
        for position, artist in enumerate(artists[:2]):
            result = self.mb('release-group',artist=artist,type='album',limit=6,inc='artist-credits')
            groups = [item for item in (result or {}).get('release-groups',[])
                      if not set(item.get('secondary-types',[])) & {'Live','Compilation','Remix'}]
            groups.sort(key=lambda item:item.get('first-release-date',''),reverse=True)
            for album in groups[:2 if position==0 else 1]:
                add('ALBUM',album.get('id'))
            result = self.mb('recording',artist=artist,limit=6,inc='artist-credits')
            tracks = [item for item in (result or {}).get('recordings',[])
                      if artist in {credit.get('artist',{}).get('id') for credit in item.get('artist-credit',[]) if isinstance(credit,dict)}]
            for track in tracks[:2 if position==0 else 1]:
                add('SONG',track.get('id'))
        maximum = min(12,max(1,int(os.environ.get('MUSIC_CATALOG_CANDIDATES_PER_SEED','8'))))
        return candidates[:maximum]
