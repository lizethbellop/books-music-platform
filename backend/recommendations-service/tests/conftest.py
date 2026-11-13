
import pytest

@pytest.fixture(autouse=True)
def disable_catalog_worker_in_unit_tests(monkeypatch):
    monkeypatch.setenv("BOOK_CATALOG_AUTOPREPARE", "false")
    monkeypatch.setenv("MUSIC_CATALOG_AUTOPREPARE", "false")
