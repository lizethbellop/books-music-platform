import base64
import os
import pytest
from recommender.serve import configure


def test_reuses_only_existing_jwt_key_without_overwriting_environment(tmp_path, monkeypatch):
    monkeypatch.delenv("JWT_SECRET", raising=False)
    monkeypatch.delenv("MODEL_PATH", raising=False)
    monkeypatch.delenv("BOOKS_DB_PASSWORD", raising=False)
    secret = base64.b64encode(b"k"*64).decode()
    auth = tmp_path/"books.env"
    auth.write_text(f"JWT_SECRET={secret}\nBOOKS_DB_PASSWORD=do-not-import\n")
    configure(tmp_path/"absent.env", auth)
    assert os.environ["JWT_SECRET"] == secret
    assert "BOOKS_DB_PASSWORD" not in os.environ
    monkeypatch.setenv("JWT_SECRET", base64.b64encode(b"x"*64).decode())
    configure(tmp_path/"absent.env", auth)
    assert os.environ["JWT_SECRET"] != secret


def test_missing_or_short_key_fails_closed(tmp_path, monkeypatch):
    monkeypatch.setenv("JWT_SECRET", "")
    monkeypatch.delenv("MODEL_PATH", raising=False)
    with pytest.raises(ValueError, match="JWT_SECRET"):
        configure(tmp_path/"absent.env")
