"""Portable JSON/NPZ artifacts; no executable pickle loading."""
import hashlib
import json
from pathlib import Path

import numpy as np
from scipy import sparse
from .model import Model


def digest(path):
    h = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(1024*1024), b""):
            h.update(block)
    return h.hexdigest()


def save(model, path, metadata):
    path = Path(path)
    path.mkdir(parents=True, exist_ok=False)
    (path / "catalog.json").write_text(json.dumps(dict(ids=model.ids, titles=model.titles, external_ids=model.external_ids)))
    sparse.save_npz(path / "content.npz", model.content)
    sparse.save_npz(path / "neighbors.npz", model.neighbors)
    np.savez_compressed(path / "arrays.npz", factors=model.factors, popularity=model.popularity,
                        explicit_factors=model.explicit_factors, item_bias=model.item_bias, global_mean=model.global_mean)
    hashes = {p.name: digest(p) for p in path.iterdir()}
    (path / "manifest.json").write_text(json.dumps(dict(schema=1, **metadata, files=hashes), indent=2))


def load(path):
    path = Path(path)
    metadata = json.loads((path / "manifest.json").read_text())
    if metadata["schema"] != 1:
        raise ValueError("unsupported artifact schema")
    expected = {"catalog.json", "content.npz", "neighbors.npz", "arrays.npz"}
    if set(metadata["files"]) != expected:
        raise ValueError("invalid artifact file list")
    for name, checksum in metadata["files"].items():
        if digest(path / name) != checksum:
            raise ValueError("artifact checksum mismatch")
    catalog = json.loads((path / "catalog.json").read_text())
    with np.load(path / "arrays.npz", allow_pickle=False) as arrays:
        model = Model(**catalog, content=sparse.load_npz(path / "content.npz"),
                      neighbors=sparse.load_npz(path / "neighbors.npz"),
                      factors=arrays["factors"], popularity=arrays["popularity"],
                      explicit_factors=arrays["explicit_factors"], item_bias=arrays["item_bias"],
                      global_mean=float(arrays["global_mean"]))
    return model, metadata
