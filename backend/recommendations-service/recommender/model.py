from dataclasses import dataclass, field

import numpy as np
from scipy import sparse
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.neighbors import NearestNeighbors
from sklearn.decomposition import NMF
from sklearn.preprocessing import normalize


@dataclass
class Model:
    ids: list[str]
    titles: list[str]
    content: sparse.csr_matrix
    neighbors: sparse.csr_matrix
    factors: np.ndarray
    popularity: np.ndarray
    explicit_factors: np.ndarray
    item_bias: np.ndarray
    global_mean: float
    external_ids: dict[str, str] = field(default_factory=dict)

    def __post_init__(self):
        self._index = {v: i for i, v in enumerate(self.ids)}
        self._ids_array = np.asarray(self.ids)

    def recommend(self, seeds=(), exclude=(), limit=10, algorithm="hybrid", allowed=None):
        index = self._index
        selected = sorted({index[s] for s in seeds if s in index})
        score = self.popularity.copy()
        reason = "popularity"
        if selected and algorithm != "popularity":
            if algorithm == "hybrid":
                content = np.asarray((self.content @ self.content[selected].mean(axis=0).T)).ravel()
            if algorithm in {"hybrid", "knn"}:
                knn = np.asarray(self.neighbors[selected].mean(axis=0)).ravel()
            if algorithm == "nmf":
                mf = self.factors @ self.factors[selected].mean(axis=0)
            def scale(x):
                return x / x.max() if x.max() > 0 else x
            if algorithm == "hybrid":
                score = .55 * scale(content) + .35 * scale(knn) + .10 * score
            elif algorithm == "knn":
                score = .9 * scale(knn) + .1 * score
            elif algorithm == "nmf":
                score = .9 * scale(mf) + .1 * score
            elif algorithm == "mf":
                # Fold in a new session from positive seeds, without a Goodbooks user ID.
                q = self.explicit_factors[selected]
                target = 5.0 - self.global_mean - self.item_bias[selected]
                user_bias = target.mean()
                profile = np.linalg.solve(q.T @ q + .1*np.eye(q.shape[1]), q.T @ (target-user_bias))
                score = self.global_mean + user_bias + self.item_bias + self.explicit_factors @ profile
            else:
                raise ValueError("unknown algorithm")
            reason = algorithm
        blocked = set(seeds) | set(exclude)
        order = np.lexsort((self._ids_array, -score))
        result = []
        for i in order:
            if self.ids[i] not in blocked and (allowed is None or self.ids[i] in allowed):
                result.append(dict(item_id=self.ids[i], title=self.titles[i], score=float(score[i]), reason=reason))
                if len(result) == limit:
                    break
        return result


def fit(catalog, ratings, k=40, seed=42, include_nmf=True):
    ids = catalog.item_id.tolist()
    index = {v: i for i, v in enumerate(ids)}
    users = {v: i for i, v in enumerate(sorted(ratings.user_id.unique()))}
    positive = ratings[ratings.rating >= 4]
    x = sparse.csr_matrix((np.ones(len(positive)),
                           ([users[v] for v in positive.user_id],
                            [index[v] for v in positive.item_id])),
                          shape=(len(users), len(ids)), dtype=np.float64)
    item_users = normalize(x.T.tocsr())
    # Bounded batches: never materialize the complete item-by-item similarity matrix.
    nn = NearestNeighbors(metric="cosine", algorithm="brute", n_jobs=1).fit(item_users)
    rows, cols, values = [], [], []
    for start in range(0, len(ids), 128):
        distances, indices = nn.kneighbors(item_users[start:start+128], n_neighbors=min(k+1, len(ids)))
        for offset, (ds, js) in enumerate(zip(distances, indices)):
            accepted = 0
            for d, j in zip(ds, js):
                if j != start+offset and d < 1 and accepted < k:
                    rows.append(start+offset); cols.append(j); values.append(1-d)
                    accepted += 1
    graph = sparse.csr_matrix((values, (rows, cols)), shape=(len(ids), len(ids)))
    text = (catalog.title + " " + catalog.authors + " " + catalog.tags).str.strip()
    content = TfidfVectorizer(max_features=30000, strip_accents="unicode").fit_transform(text)
    # Nonnegative implicit factorization baseline; not the notebook's explicit SGD MF.
    if x.nnz and include_nmf:
        nmf = NMF(n_components=min(32, *x.shape), init="random", random_state=seed, max_iter=200)
        nmf.fit(x)
        factors = normalize(nmf.components_.T)
    else:
        factors = np.zeros((len(ids), 1))
    pop = np.asarray(x.sum(axis=0)).ravel()
    pop = pop / max(float(pop.max()), 1)
    # Explicit biased SGD baseline from the notebook: 20 factors, 3 epochs.
    # Kept offline; serving only retains item factors/biases, no training user IDs.
    rng = np.random.default_rng(seed)
    p = rng.normal(0, .1, (len(users), 20))
    q = rng.normal(0, .1, (len(ids), 20))
    ub, ib = np.zeros(len(users)), np.zeros(len(ids))
    mean = float(ratings.rating.mean())
    us = np.array([users[v] for v in ratings.user_id])
    it = np.array([index[v] for v in ratings.item_id])
    rs = ratings.rating.to_numpy()
    for _ in range(3):
        for pos in rng.permutation(len(rs)):
            u, i = us[pos], it[pos]
            error = rs[pos] - (mean + ub[u] + ib[i] + p[u] @ q[i])
            old = p[u].copy()
            ub[u] += .01*(error-.02*ub[u])
            ib[i] += .01*(error-.02*ib[i])
            p[u] += .01*(error*q[i]-.02*p[u])
            q[i] += .01*(error*old-.02*q[i])
    counts = np.bincount(it, minlength=len(ids))
    q[counts == 0] = 0  # No random latent scores for unseen items.
    return Model(ids, catalog.title.tolist(), content.tocsr(), graph, factors, pop, q, ib, mean)
