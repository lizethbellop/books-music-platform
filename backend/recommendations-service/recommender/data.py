import pandas as pd
import numpy as np


def load(catalog_path, ratings_path):
    catalog = pd.read_csv(catalog_path, dtype=str).fillna("")
    ratings = pd.read_csv(ratings_path, dtype={"user_id": str, "item_id": str})
    if not {"item_id", "title", "authors", "tags"} <= set(catalog):
        raise ValueError("catalog requires item_id,title,authors,tags")
    if not {"user_id", "item_id", "rating"} <= set(ratings):
        raise ValueError("ratings requires user_id,item_id,rating")
    if catalog.empty or catalog.item_id.duplicated().any() or (catalog.item_id.str.strip() == "").any():
        raise ValueError("empty catalog or invalid/duplicate item IDs")
    if ratings.empty or ratings[["user_id", "item_id", "rating"]].isna().any().any():
        raise ValueError("empty ratings or missing values")
    ratings["rating"] = pd.to_numeric(ratings.rating, errors="raise")
    if not np.isfinite(ratings.rating).all() or not ratings.rating.between(1, 5).all():
        raise ValueError("rating must be finite in [1,5]")
    if not set(ratings.item_id) <= set(catalog.item_id):
        raise ValueError("ratings reference unknown catalog IDs")
    if (ratings.user_id.str.strip() == "").any():
        raise ValueError("empty user ID")
    # Same pair must never span partitions. Without timestamps, average duplicates.
    ratings = ratings.groupby(["user_id", "item_id"], as_index=False).rating.mean()
    return catalog.sort_values("item_id").reset_index(drop=True), ratings


def split(ratings, seed=42):
    """Three disjoint pair partitions; random, not a temporal/future guarantee."""
    rng = np.random.default_rng(seed)
    train, valid, test = [], [], []
    for _, group in ratings.groupby("user_id", sort=True):
        positions = rng.permutation(group.index)
        if len(positions) >= 3:
            test.append(positions[0]); valid.append(positions[1]); train.extend(positions[2:])
        else:
            train.extend(positions)
    return tuple(ratings.loc[ix].reset_index(drop=True) for ix in (train, valid, test))
