import argparse
import importlib.metadata
import json
import platform
from pathlib import Path
import hashlib
import time

import numpy as np
import pandas as pd
from .data import load, split
from .model import fit
from .artifacts import save, digest


def evaluate(model, history, heldout, algorithm, k=10, user_ids=None):
    if user_ids is not None:
        heldout = heldout[heldout.user_id.isin(user_ids)]
        history = history[history.user_id.isin(user_ids)]
    recall, ndcg, latencies, covered = [], [], [], set()
    histories = {u: g for u, g in history.groupby("user_id")}
    for user, group in heldout.groupby("user_id"):
        relevant = set(group.loc[group.rating >= 4, "item_id"])
        if not relevant:
            continue
        past = histories.get(user, history.iloc[:0])
        start = time.perf_counter()
        result = model.recommend(past.loc[past.rating >= 4, "item_id"].tolist(),
                                 past.item_id.tolist(), k, algorithm)
        latencies.append((time.perf_counter()-start)*1000)
        ids = [r["item_id"] for r in result]
        covered.update(ids)
        recall.append(len(set(ids) & relevant)/len(relevant))
        dcg = sum(1/np.log2(i+2) for i, v in enumerate(ids) if v in relevant)
        ideal = sum(1/np.log2(i+2) for i in range(min(k, len(relevant))))
        ndcg.append(dcg/ideal)
    return dict(users=len(recall), recall_at_10=float(np.mean(recall)) if recall else None,
                ndcg_at_10=float(np.mean(ndcg)) if ndcg else None,
                coverage=len(covered)/len(model.ids),
                p95_ms=float(np.percentile(latencies, 95)) if latencies else None)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", required=True)
    parser.add_argument("--ratings", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--domain", choices=["books", "music"], default="books")
    parser.add_argument("--namespace", required=True)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--eval-users", type=int, default=1000, help="Seeded held-out user sample; 0 means all users")
    parser.add_argument("--include-nmf", action="store_true", help="Also compare the more expensive implicit NMF baseline")
    parser.add_argument("--crosswalk", help="Goodbooks -> OpenLibrary mapping CSV; matched rows only")
    args = parser.parse_args()
    if Path(args.output).exists():
        parser.error("output already exists; choose a new model version")
    if args.eval_users < 0:
        parser.error("--eval-users must be nonnegative")
    started = time.perf_counter()
    catalog, ratings = load(args.catalog, args.ratings)
    train, valid, test = split(ratings, args.seed)
    eligible = sorted(valid.user_id.unique())
    rng = np.random.default_rng(args.seed)
    selected_users = sorted(rng.choice(eligible, min(args.eval_users or len(eligible), len(eligible)), replace=False).tolist())
    algorithms = ["popularity", "knn", "mf", "hybrid"]
    if args.include_nmf:
        algorithms.insert(-1, "nmf")
    print(f"Training {len(train)} pairs; validation users sampled: {len(selected_users)}/{len(eligible)}", flush=True)
    model = fit(catalog, train, seed=args.seed, include_nmf=args.include_nmf)
    validation = {}
    for algorithm in algorithms:
        validation[algorithm] = evaluate(model, train, valid, algorithm, user_ids=selected_users)
        print(f"Validation {algorithm}: {validation[algorithm]}", flush=True)
    if not any(v["users"] for v in validation.values()):
        raise ValueError("No positive validation targets: provide more interactions")
    winner = max(validation, key=lambda a: validation[a]["ndcg_at_10"])
    print(f"Frozen winner: {winner}; refitting train+validation for test", flush=True)
    development = pd.concat([train, valid], ignore_index=True)
    test_model = fit(catalog, development, seed=args.seed, include_nmf=args.include_nmf)
    test_metrics = evaluate(test_model, development, test, winner, user_ids=selected_users)
    baseline_test = evaluate(test_model, development, test, "popularity", user_ids=selected_users)
    print(f"Test: {test_metrics}; refitting all pairs for serving", flush=True)
    production = fit(catalog, ratings, seed=args.seed, include_nmf=args.include_nmf)
    source_hashes = {"catalog": digest(args.catalog), "ratings": digest(args.ratings)}
    if args.crosswalk:
        if args.domain != "books" or args.namespace != "goodbooks":
            raise ValueError("Goodbooks crosswalk requires --domain books --namespace goodbooks")
        from .openlibrary import load_crosswalk
        production.external_ids = load_crosswalk(args.crosswalk, production.ids)
        source_hashes["crosswalk"] = digest(args.crosswalk)
    metadata = dict(version=args.output.rstrip("/").split("/")[-1], domain=args.domain,
                    namespace=args.namespace, algorithm=winner, seed=args.seed,
                    sources=source_hashes,
                    serving_namespace="openlibrary-work" if args.crosswalk else args.namespace,
                    mapped_items=len(production.external_ids),
                    evaluation=dict(eligible_users=len(eligible), sampled_users=len(selected_users),
                                    sampling="seeded uniform without replacement before relevance filtering"),
                    training_seconds=time.perf_counter()-started,
                    split=dict(train=len(train), validation=len(valid), test=len(test)),
                    validation=validation, test=test_metrics, baseline_test=baseline_test,
                    code_sha256=hashlib.sha256(b"".join(p.read_bytes() for p in sorted(Path(__file__).parent.glob("*.py")))).hexdigest(),
                    hyperparameters=dict(knn_k=40, positive_threshold=4, tfidf_max_features=30000,
                                         mf_factors=20, mf_epochs=3, mf_learning_rate=.01,
                                         mf_regularization=.02, fold_in_regularization=.1,
                                         nmf_enabled=args.include_nmf, nmf_factors_max=32, nmf_max_iter=200,
                                         hybrid_weights=[.55, .35, .10]),
                    python=platform.python_version(),
                    dependencies={p: importlib.metadata.version(p) for p in ("numpy", "scipy", "scikit-learn", "pandas")})
    save(production, args.output, metadata)
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
