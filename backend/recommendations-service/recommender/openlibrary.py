"""Auditable Goodbooks -> OpenLibrary Work mapping. No network calls at serving time."""
import argparse
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
import gzip
import hashlib
import json
from pathlib import Path
import re
import time

import httpx
import pandas as pd

from .artifacts import digest
from .provider_budget import provider_request


WORK = re.compile(r"OL[0-9]+W")


def work_id(value):
    """Musa BookController expects OL...W, without /works/. Editions are rejected."""
    value = str(value).removeprefix("/works/")
    return value if WORK.fullmatch(value) else None


def isbn13(value):
    """Validate checksum; recover numeric formatting without inventing missing digits."""
    value = str(value).strip().upper().replace("-", "").replace(" ", "")
    if not value or value in {"NAN", "NONE"}:
        return None
    if "." in value or "E" in value:
        try:
            number = Decimal(value)
            if not number.is_finite() or number != number.to_integral_value():
                return None
            value = format(number, "f").split(".")[0]
        except InvalidOperation:
            return None
    if len(value) == 9 and value.isdigit():
        value = "0" + value  # Goodbooks dropped a leading zero from numeric ISBN-10.
    if len(value) == 10 and value[:9].isdigit() and (value[-1].isdigit() or value[-1] == "X"):
        digits = [int(c) if c != "X" else 10 for c in value]
        if sum((10-i)*d for i, d in enumerate(digits)) % 11:
            return None
        prefix = "978" + value[:9]
        check = (-sum(int(d)*(1 if i % 2 == 0 else 3) for i, d in enumerate(prefix))) % 10
        return prefix + str(check)
    if len(value) == 13 and value.isdigit() and value.startswith(("978", "979")):
        if sum(int(d)*(1 if i % 2 == 0 else 3) for i, d in enumerate(value)) % 10 == 0:
            return value
    return None


def book_isbns(row):
    return {v for v in (isbn13(row.get("isbn", "")), isbn13(row.get("isbn13", ""))) if v}


def read_records(path):
    """Stream official editions TSV(.gz), or JSONL snapshots for smaller exports."""
    opener = gzip.open if str(path).endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as stream:
        for line in stream:
            yield json.loads(line.split("\t", 4)[-1])


def collect_dump(path, targets):
    candidates = {isbn: {} for isbn in targets}
    for record in read_records(path):
        isbns = {isbn13(v) for v in (record.get("isbn_10") or []) + (record.get("isbn_13") or [])} & targets
        if not isbns:
            continue
        works = {work_id(v.get("key")) for v in record.get("works", [])} - {None}
        for isbn in isbns:
            for work in works:
                candidates[isbn][work] = record.get("key", "")
    return candidates


def collect_search(payload, targets):
    docs = payload.get("docs", [])
    total = payload.get("numFound", payload.get("num_found"))
    if total is None or total > len(docs) or payload.get("start", 0) != 0:
        raise ValueError("Incomplete search results; use a dump or a smaller batch")
    candidates = {isbn: {} for isbn in targets}
    for record in docs:
        work = work_id(record.get("key"))
        if not work:
            continue
        matched = {isbn13(v) for v in record.get("isbn", [])} & targets
        for isbn in matched:
            candidates[isbn][work] = record.get("key", "")
    return candidates


def resolve(books, candidates, source):
    rows = []
    for row in books.to_dict("records"):
        isbns = book_isbns(row)
        works = {w for isbn in isbns for w in candidates.get(isbn, {})}
        status = "matched" if len(works) == 1 else "ambiguous" if works else "unresolved" if isbns else "invalid_isbn"
        chosen = next(iter(works)) if status == "matched" else ""
        evidence = sorted({v for isbn in isbns for w, v in candidates.get(isbn, {}).items() if w == chosen})
        rows.append(dict(item_id=str(row["id"]), work_id=chosen, status=status,
                         isbn13="|".join(sorted(isbns)), evidence="|".join(evidence), source=source))
    return pd.DataFrame(rows)


def search_snapshot(targets, cache, user_agent):
    """Small integration lookup, not a bulk harvesting job. Cache exact responses."""
    query = " OR ".join("isbn:" + isbn for isbn in sorted(targets))
    params = dict(q=query, fields="key,title,isbn,author_name", limit=100)
    key = hashlib.sha256(json.dumps(params, sort_keys=True).encode()).hexdigest()
    cache = Path(cache)
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / (key + ".json")
    if path.exists():
        snapshot = json.loads(path.read_text())
        return snapshot["response"], path
    with httpx.Client(timeout=30, follow_redirects=True, headers={"User-Agent": user_agent}) as client:
        for attempt in range(3):
            response = provider_request(client, "openlibrary", "GET", "https://openlibrary.org/search.json", params=params)
            if response.status_code == 429 or response.status_code >= 500:
                if attempt < 2:
                    time.sleep(min(30, max(1, 2**attempt)))
                    continue
            response.raise_for_status()
            payload = response.json()
            snapshot = dict(url=str(response.url), retrieved_at=datetime.now(timezone.utc).isoformat(), response=payload)
            path.write_text(json.dumps(snapshot, ensure_ascii=False))
            return payload, path
    raise RuntimeError("OpenLibrary lookup failed")


def load_crosswalk(path, item_ids):
    frame = pd.read_csv(path, dtype=str).fillna("")
    if not {"item_id", "work_id", "status"} <= set(frame):
        raise ValueError("Crosswalk requires item_id,work_id,status")
    if frame.item_id.duplicated().any() or not set(frame.item_id) <= set(item_ids):
        raise ValueError("Crosswalk contains duplicate/unknown Goodbooks IDs")
    matched = frame[frame.status == "matched"]
    if any(work_id(v) != v for v in matched.work_id):
        raise ValueError("Crosswalk must contain bare OL...W Work IDs")
    return dict(zip(matched.item_id, matched.work_id))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--books", required=True)
    parser.add_argument("--output", required=True)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--editions-dump", help="Local official editions TSV(.gz) or JSONL snapshot")
    source.add_argument("--lookup-sample", type=int, help="Small live ISBN lookup, 1..50 books; use dumps for full mapping")
    parser.add_argument("--cache", default="data/processed/openlibrary-cache")
    parser.add_argument("--user-agent", default="musa-recommendations/0.2 (educational ISBN lookup)")
    args = parser.parse_args()
    books = pd.read_csv(args.books, dtype=str).fillna("")
    selected = books
    if args.lookup_sample is not None:
        if not 1 <= args.lookup_sample <= 50:
            parser.error("--lookup-sample must be 1..50; use an editions dump for the full catalog")
        selected = books.head(args.lookup_sample)
    targets = {isbn for row in selected.to_dict("records") for isbn in book_isbns(row)}
    if args.editions_dump:
        source_path = Path(args.editions_dump)
        candidates = collect_dump(source_path, targets)
    else:
        payload, source_path = search_snapshot(targets, args.cache, args.user_agent)
        candidates = collect_search(payload, targets)
    # Write every book including unresolved ones; never pretend partial coverage is complete.
    frame = resolve(books, candidates, str(source_path))
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    frame.to_csv(output, index=False)
    report = dict(books=len(books), queried_books=len(selected), statuses=frame.status.value_counts().to_dict(),
                  sources={"books": digest(args.books), "openlibrary_snapshot": digest(source_path)})
    output.with_suffix(".report.json").write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
