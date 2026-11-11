"""Explicit adapter: Goodbooks ratings.book_id points to books.id, NOT books.book_id."""
import argparse
from pathlib import Path
import pandas as pd


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--raw", required=True)
    p.add_argument("--output", required=True)
    a = p.parse_args()
    raw, out = Path(a.raw), Path(a.output)
    books = pd.read_csv(raw / "books.csv", dtype=str).fillna("")
    ratings = pd.read_csv(raw / "ratings.csv", dtype=str)
    catalog = books.rename(columns={"id": "item_id"})[["item_id", "title", "authors"]].copy()
    # Only intrinsic metadata; community tags/counts may contain post-split behavior.
    catalog["tags"] = ""
    ratings = ratings.rename(columns={"book_id": "item_id"})
    out.mkdir(parents=True, exist_ok=True)
    catalog.to_csv(out / "catalog.csv", index=False)
    ratings[["user_id", "item_id", "rating"]].to_csv(out / "ratings.csv", index=False)


if __name__ == "__main__":
    main()
