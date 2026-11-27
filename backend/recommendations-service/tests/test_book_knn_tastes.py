import pytest

from recommender.service.book_knn import recommend_from_catalog, feature_vector
from recommender.service.book_tastes import (
    BookRating, BookSignals, ProfileSeeds, BookTastes, build_tastes,
)


def features(**values):
    result = {k: [] for k in ("genres", "themes", "settings", "tropes", "audience", "formats")}
    result.update(values)
    return result


def book(oid, author="Author", **values):
    return {
        "externalId": oid,
        "metadata": {"title": oid, "authors": [author]},
        "features": features(**values),
    }


def test_ratings_override_lists_and_exclusions_include_all_known_books():
    tastes = build_tastes(
        ProfileSeeds(bookIds=["OL1W", "OL2W", "OL3W", "OL3W"]),
        BookSignals(
            ratings=[
                BookRating(externalId="OL1W", rating=3),
                BookRating(externalId="OL2W", rating=4),
                BookRating(externalId="OL4W", rating=5),
            ],
            libraryIds=["OL5W"],
        ),
    )
    assert tastes.seeds == {"OL2W": 2, "OL3W": 1, "OL4W": 3}
    assert tastes.exclude == {"OL1W", "OL2W", "OL3W", "OL4W", "OL5W"}


def test_knn_prefers_matching_themes_and_excludes_known_books():
    catalog = [
        book("OL1W", themes=["STEM"], genres=["romance"]),
        book("OL2W", themes=["stem"], genres=["romance"]),
        book("OL3W", themes=["murder"], genres=["mystery"]),
        book("OL4W", themes=["stem"], genres=["romance"]),
    ]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1}, {"OL1W", "OL4W"}))
    assert [b["externalId"] for b in result["items"]] == ["OL2W"]
    assert result["items"][0]["sourceBookId"] == "OL1W"
    assert result["items"][0]["score"] == 1
    assert "themes:stem" in result["items"][0]["matchedFeatures"]


def test_audience_and_format_alone_do_not_make_a_recommendation():
    catalog = [
        book("OL1W", themes=["stem"], audience=["adult"], formats=["novel"]),
        book("OL2W", themes=["murder"], audience=["adult"], formats=["novel"]),
    ]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1}, {"OL1W"}))
    assert result["items"] == []


def test_empty_catalog_is_explicit():
    result = recommend_from_catalog([], BookTastes({"OL1W": 1}, {"OL1W"}))
    assert result["mode"] == "catalog_not_ready"
    assert result["missing_seed_ids"] == ["OL1W"]


def test_missing_seeds_are_not_disguised_as_personalized_results():
    result = recommend_from_catalog(
        [book("OL2W", themes=["stem"])], BookTastes({"OL1W": 1}, {"OL1W"})
    )
    assert result["mode"] == "profile_not_prepared"
    assert result["items"] == []


def test_cold_start_is_explore_and_still_excludes_library():
    catalog = [book("OL1W", themes=["stem"]), book("OL2W", themes=["romance"])]
    result = recommend_from_catalog(catalog, BookTastes({}, {"OL1W"}))
    assert result["mode"] == "explore"
    assert [b["externalId"] for b in result["items"]] == ["OL2W"]


def test_partial_profile_reports_missing_seeds():
    catalog = [book("OL1W", themes=["stem"]), book("OL2W", themes=["stem"])]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1, "OL9W": 2}, {"OL1W", "OL9W"}))
    assert result["partial"] is True
    assert result["usable_seed_count"] == 1
    assert result["missing_seed_ids"] == ["OL9W"]
    assert len(result["items"]) == 1


def test_diversity_defers_third_book_by_same_author():
    catalog = [book("OL1W", themes=["stem"])] + [
        book("OL2W", author="A", themes=["stem"]),
        book("OL3W", author="A", themes=["stem"]),
        book("OL4W", author="A", themes=["stem"]),
        book("OL5W", author="B", themes=["stem"]),
    ]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1}, {"OL1W"}), limit=3)
    assert [b["externalId"] for b in result["items"]] == ["OL2W", "OL3W", "OL5W"]


def test_zero_information_books_are_not_ranked():
    result = recommend_from_catalog([book("OL1W")], BookTastes({}, set()))
    assert result["catalog_size"] == 0


def test_more_five_star_affinity_has_more_weight_than_list_affinity():
    catalog = [
        book("OL1W", themes=["stem"]),
        book("OL2W", themes=["history"]),
        book("OL3W", themes=["stem"]),
        book("OL4W", themes=["history"]),
    ]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1, "OL2W": 3}, {"OL1W", "OL2W"}))
    assert [b["externalId"] for b in result["items"]] == ["OL4W", "OL3W"]


def test_duplicate_labels_do_not_inflate_a_vector():
    assert feature_vector(features(themes=["STEM", "stem"])) == feature_vector(features(themes=["stem"]))


def test_duplicate_ratings_are_rejected():
    with pytest.raises(ValueError):
        build_tastes(
            ProfileSeeds(bookIds=[]),
            BookSignals(ratings=[BookRating(externalId="OL1W", rating=4)] * 2, libraryIds=[]),
        )


def test_equivalent_genre_spelling_matches_without_changing_saved_features():
    catalog = [
        book("OL1W", genres=["romance fiction", "literary fiction", "coming of age"]),
        book("OL2W", genres=["romance", "literary", "coming-of-age"]),
        book("OL3W", genres=["coming‑of‑age", "literary-fiction"]),
    ]
    result = recommend_from_catalog(catalog, BookTastes({"OL1W": 1}, {"OL1W"}))
    assert [item["externalId"] for item in result["items"]] == ["OL2W", "OL3W"]
    assert result["items"][0]["score"] == 1
    assert catalog[0]["features"]["genres"][0] == "romance fiction"


def test_favorites_are_positive_but_low_ratings_override_them():
    tastes=build_tastes(ProfileSeeds(bookIds=[]),BookSignals(
        ratings=[BookRating(externalId='OL2W',rating=2)],libraryIds=[],favoriteIds=['OL1W','OL2W']))
    assert tastes.seeds=={'OL1W':1.5}
    assert tastes.exclude=={'OL1W','OL2W'}


@pytest.mark.parametrize('generic_genre',['fiction','non-fiction','nonfiction'])
def test_generic_fiction_alone_is_not_a_personalized_match(generic_genre):
    catalog=[book('OL1W',genres=[generic_genre],themes=['stem']),
             book('OL2W',genres=[generic_genre],themes=['crime'])]
    result=recommend_from_catalog(catalog,BookTastes({'OL1W':1},{'OL1W'}))
    assert result['items']==[]
