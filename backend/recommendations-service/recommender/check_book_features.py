"""Comprueba el repositorio en una tabla temporal, sin alterar libros reales."""
import hashlib
from contextlib import contextmanager

from . import book_features_repository as repository
from .database import get_connection


def main():
    original = repository.get_connection
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                "CREATE TEMP TABLE book_features "
                "(LIKE public.book_features INCLUDING ALL)"
            )

        @contextmanager
        def temporary_connection():
            yield connection

        repository.get_connection = temporary_connection
        try:
            work_id = "OL26350090W"
            source_hash = hashlib.sha256(b"manual-storage-check-v1").hexdigest()
            repository.save_book_features(
                work_id, {"themes": ["STEM"]}, source_hash,
                "manual-check-v1", "manual-check",
            )
            saved = repository.get_book_features(work_id)
            assert saved is not None
            assert saved["features"] == {"themes": ["STEM"]}
            print("Guardado y consulta correctos")

            repository.save_book_features(
                work_id, {"themes": ["STEM", "romance"]}, source_hash,
                "manual-check-v2", "manual-check",
            )
            updated = repository.get_book_features(work_id)
            assert updated is not None
            assert updated["features"] == {"themes": ["STEM", "romance"]}
            assert updated["extractor_version"] == "manual-check-v2"
            print("Actualización correcta")
        finally:
            repository.get_connection = original
    print("Comprobación terminada; los libros reales no cambiaron")


if __name__ == "__main__":
    main()
