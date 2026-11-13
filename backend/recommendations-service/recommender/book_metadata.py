import hashlib
import json


def prepare_book_metadata(book: dict) -> dict:
    return {
        "title": book["title"].strip(),
        "authors": sorted({
            author.strip()
            for author in (book.get("authors") or [])
            if author.strip()
        }),
        "subjects": sorted({
            subject.strip()
            for subject in (book.get("subjects") or [])
            if subject.strip()
        }),
        "description": (book.get("description") or "").strip()[:6000],
    }


def calculate_source_hash(metadata: dict) -> str:
    serialized = json.dumps(
        metadata,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )

    return hashlib.sha256(
        serialized.encode("utf-8")
    ).hexdigest()