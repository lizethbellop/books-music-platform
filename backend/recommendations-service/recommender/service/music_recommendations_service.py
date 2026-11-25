from ..music_features_repository import list_music_catalog, canonical_music_keys
from .music_knn import recommend_music
from .music_tastes import get_music_tastes, MusicTastes
from .music_catalog_jobs import enqueue_music


def recommend_music_for_user(token: str, content_type: str, limit: int = 12):
    tastes = get_music_tastes(token)
    aliases = canonical_music_keys(set(tastes.seeds) | tastes.exclude)
    seeds = {}
    for key, weight in tastes.seeds.items():
        canonical=aliases.get(key,key)
        seeds[canonical]=max(seeds.get(canonical,0),weight)
    exclude=tastes.exclude | {aliases.get(key,key) for key in tastes.exclude}
    result=recommend_music(list_music_catalog(),MusicTastes(seeds,exclude),content_type,limit)
    result['preparation']=enqueue_music(tastes.seeds)
    return result


def recommend_similar_music(content_type: str, external_id: str, limit: int = 8):
    from ..music_models import MusicRef
    ref=MusicRef(contentType=content_type,externalId=external_id)
    aliases=canonical_music_keys([ref.key])
    canonical=aliases.get(ref.key,ref.key)
    result=recommend_music(list_music_catalog(),MusicTastes({canonical:1},{canonical,ref.key}),content_type,limit)
    if result['mode']=='profile_not_prepared':
        result['mode']='reference_not_prepared'
    result['reference_item_key']=ref.key
    result['preparation']=enqueue_music([ref.key])
    return result
