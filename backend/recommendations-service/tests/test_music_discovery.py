from uuid import uuid4
import pytest
from recommender.service.music_discovery import MusicDiscovery, MappingUnavailable, spotify_links


def test_mapping_requires_matching_resource_type_and_valid_spotify_id():
    oid='A'*22
    payload={'relations':[{'url':{'resource':'https://open.spotify.com/artist/'+oid}},
                          {'url':{'resource':'https://open.spotify.com/track/'+'B'*22}},
                          {'url':{'resource':'https://fake.example/artist/'+'C'*22}}]}
    assert spotify_links(payload,'ARTIST') == {oid}


def test_reverse_mapping_never_guesses_by_title(tmp_path):
    discovery=MusicDiscovery(cache_dir=tmp_path)
    mbid=str(uuid4())
    discovery.entity=lambda *args:{'id':mbid,'name':'Zara Larsson','relations':[]}
    try:
        with pytest.raises(MappingUnavailable):
            discovery.from_mbid('ARTIST',mbid)
    finally:
        discovery.close()


def test_recording_reverse_mapping_requires_exact_isrc(tmp_path):
    discovery=MusicDiscovery(cache_dir=tmp_path)
    mbid=str(uuid4())
    discovery.entity=lambda *args:{'id':mbid,'title':'Example','isrcs':['SEAAA2400001']}
    discovery.spotify=lambda *args:{'tracks':{'items':[
        {'id':'A'*22,'external_ids':{'isrc':'SEAAA2400001'}},
        {'id':'B'*22,'external_ids':{'isrc':'USAAA2400001'}}]}}
    try:
        result=discovery.from_mbid('SONG',mbid)
        assert result[0]=='A'*22
        assert result[4]=='ISRC:SEAAA2400001'
    finally:
        discovery.close()


def test_album_uses_verified_release_relationship(tmp_path):
    discovery=MusicDiscovery(cache_dir=tmp_path)
    mbid=str(uuid4())
    discovery.entity=lambda *args:{'id':mbid,'title':'Album','relations':[]}
    calls=[]
    def mb(path,**params):
        calls.append((path,params))
        return {'releases':[{'relations':[{'url':{'resource':'https://open.spotify.com/album/'+'A'*22}}]}]}
    discovery.mb=mb
    try:
        assert discovery.from_mbid('ALBUM',mbid)[0]=='A'*22
        assert calls[0][1]['release-group']==mbid
    finally:
        discovery.close()


def test_discovery_is_bounded_and_does_not_return_foreign_recordings(tmp_path):
    discovery=MusicDiscovery(cache_dir=tmp_path)
    artist,peer,group,track,wrong=[str(uuid4()) for _ in range(5)]
    def mb(path,**params):
        if path=='artist': return {'artists':[{'id':peer}]}
        if path=='release-group': return {'release-groups':[{'id':group,'first-release-date':'2025'}]}
        return {'recordings':[{'id':wrong,'artist-credit':[{'artist':{'id':wrong}}]},
                              {'id':track,'artist-credit':[{'artist':{'id':params['artist']}}]}]}
    discovery.mb=mb
    try:
        result=discovery.discover({'features':{'artists':[artist],'genres':['synth-pop']}})
        assert ('ARTIST',artist) in result and ('ARTIST',peer) in result
        assert ('SONG',wrong) not in result
        assert len(result)<=8 and len(result)==len(set(result))
    finally:
        discovery.close()
