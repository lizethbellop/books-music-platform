"""Read-only audit of every supplied Goodbooks CSV; never writes into raw/."""
import argparse
import json
from pathlib import Path

import pandas as pd

from .artifacts import digest
from .openlibrary import book_isbns


def audit(raw):
    raw = Path(raw)
    frames = {p.name: pd.read_csv(p, dtype=str).fillna("") for p in sorted(raw.glob("*.csv"))}
    books, ratings = frames["books.csv"], frames["ratings.csv"]
    counts = ratings.groupby(["user_id", "book_id"]).rating.nunique()
    known_isbns = [book_isbns(row) for row in books.to_dict("records")]
    return dict(
        files={name: dict(rows=len(frame), columns=list(frame), sha256=digest(raw/name))
               for name, frame in frames.items()},
        books=len(books), users=int(ratings.user_id.nunique()),
        ratings=len(ratings), clean_pairs=len(counts),
        duplicate_pair_rows=int(ratings.duplicated(["user_id", "book_id"]).sum()),
        conflicting_pairs=int((counts > 1).sum()),
        duplicate_goodreads_works=int(books.work_id.duplicated().sum()),
        ratings_outside_catalog=len(set(ratings.book_id)-set(books.id)),
        books_with_valid_isbn=sum(bool(v) for v in known_isbns),
        books_without_valid_isbn=sum(not v for v in known_isbns),
        notes={"ratings.csv": "Training signal, 1..5. Deduplicate before pair split.",
               "books.csv": "id is ratings FK; book_id and work_id are Goodreads IDs, not OpenLibrary IDs.",
               "book_tags.csv": "Joins books.book_id using goodreads_book_id; excluded from leakage-sensitive training.",
               "tags.csv": "Community vocabulary; no user-time provenance, excluded from current model.",
               "to_read.csv": "Wishlist snapshot, not a rating of 5; excluded from training and evaluation.",
               "sample_book.xml": "Example document, not a ratings source; preserved and not executed."})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    report = audit(args.raw)
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False))
    print(json.dumps({k: v for k, v in report.items() if k not in {"files", "notes"}}, indent=2))


if __name__ == "__main__":
    main()
