import base64
import asyncio
import httpx
from contextlib import asynccontextmanager
import os
from uuid import UUID

import jwt
from fastapi import FastAPI, Depends, HTTPException, Query, Response
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field, ConfigDict

from .artifacts import load


class RecommendationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    seeds: list[str] = Field(default_factory=list, max_length=100)
    exclude: list[str] = Field(default_factory=list, max_length=1000)
    limit: int = Field(default=10, ge=1, le=100)


def create_app(artifact_path=None, secret=None, upstream_transport=None):
    @asynccontextmanager
    async def lifespan(app):
        key = base64.b64decode(secret or os.environ["JWT_SECRET"], validate=True)
        if len(key) < 64:
            raise ValueError("HS512 requires at least 64 decoded key bytes")
        path = artifact_path or os.environ.get("MODEL_PATH")
        model, metadata = load(path) if path else (None, dict(
            version="openlibrary-content-v1", domain="books", namespace="openlibrary-work", algorithm="content"))
        app.state.key, app.state.model, app.state.metadata = key, model, metadata
        from .home import HomeRecommender
        async with httpx.AsyncClient(timeout=6, transport=upstream_transport,
                                     limits=httpx.Limits(max_connections=10)) as client:
            app.state.home = HomeRecommender(client, model, metadata,
                os.environ.get("PROFILE_SERVICE_URL", "http://localhost:8082"),
                os.environ.get("BOOKS_SERVICE_URL", "http://localhost:8081"))
            yield

    app = FastAPI(title="musa recommendations", lifespan=lifespan)
    bearer = HTTPBearer(auto_error=False)

    def authenticate(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
        try:
            if not credentials or credentials.scheme.lower() != "bearer":
                raise ValueError("missing bearer")
            claims = jwt.decode(credentials.credentials, app.state.key, algorithms=["HS512"],
                                issuer="musa-auth", audience="musa-api",
                                options={"require": ["sub", "exp", "iss", "aud"]})
            if str(UUID(claims["sub"])) != claims["sub"].lower():
                raise ValueError("invalid subject")
            return claims["sub"]
        except (jwt.PyJWTError, ValueError, TypeError, AttributeError):
            raise HTTPException(401, "Token inválido o expirado", headers={"WWW-Authenticate": "Bearer"})

    @app.get("/health/live")
    def live():
        return {"status": "ok"}

    @app.get("/health/ready")
    def ready():
        return {"status": "ready", "version": app.state.metadata["version"]}

    @app.get("/api/recommendations/books/me")
    async def home_books(response: Response, limit: int = Query(default=12, ge=1, le=20),
                         subject=Depends(authenticate), credentials=Depends(bearer)):
        response.headers["Cache-Control"] = "no-store"
        response.headers["Vary"] = "Authorization"
        try:
            async with asyncio.timeout(18):
                return await app.state.home.recommend(credentials.credentials, limit)
        except TimeoutError:
            raise HTTPException(503, "Las recomendaciones tardaron demasiado. Intenta nuevamente.")

    @app.post("/api/recommendations/{domain}")
    def recommendations(domain: str, request: RecommendationRequest, subject=Depends(authenticate)):
        meta = app.state.metadata
        if domain != meta["domain"]:
            raise HTTPException(404, "Modelo no disponible para este dominio")
        model = app.state.model
        if model is None:
            raise HTTPException(503, "Usa el endpoint de Inicio o configura un modelo entrenado")
        if meta.get("serving_namespace") == "openlibrary-work":
            from .openlibrary import work_id
            if not model.external_ids:
                raise HTTPException(503, "No hay equivalencias OpenLibrary verificadas")
            if any(work_id(v) is None for v in request.seeds + request.exclude):
                raise HTTPException(422, "Se requieren IDs de obra OpenLibrary OL...W")
            seeds = {work_id(v) for v in request.seeds}
            exclude = {work_id(v) for v in request.exclude}
            reverse = {}
            for internal, external in model.external_ids.items():
                reverse.setdefault(external, []).append(internal)
            internal_seeds = [i for v in seeds for i in reverse.get(v, [])]
            internal_exclude = [i for v in exclude for i in reverse.get(v, [])]
            ranked = model.recommend(internal_seeds, internal_exclude, len(model.ids),
                                     meta["algorithm"], allowed=model.external_ids)
            items, used = [], set()
            for row in ranked:
                external = model.external_ids[row["item_id"]]
                if external in used:
                    continue
                used.add(external)
                items.append(dict(row, goodbooks_id=row["item_id"], item_id=external, externalId=external))
                if len(items) == request.limit:
                    break
            return dict(model_version=meta["version"], namespace="openlibrary-work",
                        model_namespace=meta["namespace"],
                        mapped_items=len(model.external_ids), total_items=len(model.ids),
                        unmapped_seeds=sorted(seeds-set(reverse)), items=items)
        unknown = set(request.seeds) - set(model.ids)
        if unknown:
            raise HTTPException(422, "Semillas fuera del catálogo/namespace del modelo")
        return dict(model_version=meta["version"], namespace=meta["namespace"],
                    items=model.recommend(request.seeds, request.exclude, request.limit, meta["algorithm"]))

    return app


app = create_app()
