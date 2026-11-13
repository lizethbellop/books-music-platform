import pytest
from test_book_knn_api import api_client
from test_music_recommendations import item
from recommender.service import music_recommendations_service as service


@pytest.mark.parametrize('kind,content_type',[('artists','ARTIST'),('albums','ALBUM'),('songs','SONG')])
def test_similar_music_auth_validation_and_type(monkeypatch,kind,content_type):
    calls=[]
    monkeypatch.setattr('recommender.api.recommend_similar_music',lambda kind,oid,limit:calls.append((kind,oid,limit)) or {'items':[]})
    monkeypatch.setattr('recommender.api.recommend_music_for_user',lambda *args:pytest.fail('Must not fetch personal tastes'))
    client,headers=api_client(monkeypatch)
    with client:
        url=f'/api/recommendations/music/{kind}/reference/similar'
        assert client.get(url).status_code==401
        assert client.get(url+'?limit=21',headers=headers).status_code==422
        assert client.get(f'/api/recommendations/music/{kind}/invalid%20id/similar',headers=headers).status_code==422
        response=client.get(url+'?limit=4',headers=headers)
    assert response.status_code==200
    assert response.headers['Cache-Control']=='no-store'
    assert calls==[(content_type,'reference',4)]


@pytest.mark.parametrize('kind',['ARTIST','ALBUM','SONG'])
def test_similar_music_uses_reference_excludes_equivalent_id_and_preserves_type(monkeypatch,kind):
    monkeypatch.setattr(service,'canonical_music_keys',lambda keys:{kind+':edition':kind+':canonical'})
    monkeypatch.setattr(service,'list_music_catalog',lambda:[
        item(kind,'canonical',genres=['synth-pop']),item(kind,'neighbor',genres=['synth-pop']),
        item('SONG' if kind!='SONG' else 'ALBUM','wrong-type',genres=['synth-pop'])])
    seen=[]
    monkeypatch.setattr(service,'enqueue_music',lambda keys:seen.extend(keys) or {'status':'ready'})
    monkeypatch.setattr(service,'get_music_tastes',lambda *args:pytest.fail('Must not read preferences'))
    result=service.recommend_similar_music(kind,'edition',4)
    assert [row['spotifyId'] for row in result['items']]==['neighbor']
    assert result['reference_item_key']==kind+':edition'
    assert seen==[kind+':edition']


def test_unprepared_music_reference_is_queued_without_external_calls(monkeypatch):
    monkeypatch.setattr(service,'canonical_music_keys',lambda keys:{})
    monkeypatch.setattr(service,'list_music_catalog',lambda:[item('SONG','neighbor',genres=['synth-pop'])])
    monkeypatch.setattr(service,'enqueue_music',lambda keys:{'status':'pending'})
    result=service.recommend_similar_music('SONG','new-reference')
    assert result['mode']=='reference_not_prepared'
    assert result['items']==[]
    assert result['preparation']['status']=='pending'
