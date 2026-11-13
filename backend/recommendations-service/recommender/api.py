import asyncio
import base64
import os
from contextlib import asynccontextmanager
from uuid import UUID
from typing import Literal

import httpx
import jwt
import psycopg
from fastapi import (
    Depends,
    FastAPI,
    HTTPException,
    Query,
    Path,
    Response,
)
from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBearer,
)
from pydantic import BaseModel, ConfigDict, Field

from .service.book_catalog_jobs import BookCatalogWorker, PreparationDeferred
from .service.music_catalog_jobs import MusicCatalogWorker
from .music_models import MUSIC_KINDS
from .service.music_recommendations_service import recommend_music_for_user, recommend_similar_music
from .artifacts import load
from .service.book_recommendations_service import recommend_books_for_user, recommend_similar_books
from .feature_extractor import BookFeatures
from .home import HomeRecommender
from .openlibrary import work_id as normalize_work_id
from .service.book_features_service import get_features_for_book


class RecommendationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    seeds: list[str] = Field(
        default_factory=list,
        max_length=100,
    )
    exclude: list[str] = Field(
        default_factory=list,
        max_length=1000,
    )
    limit: int = Field(default=10, ge=1, le=100)


def create_app(
    artifact_path=None,
    secret=None,
    upstream_transport=None,
):
    @asynccontextmanager
    async def lifespan(app):
        key = base64.b64decode(
            secret or os.environ["JWT_SECRET"],
            validate=True,
        )

        if len(key) < 64:
            raise ValueError(
                "HS512 requires at least 64 decoded key bytes"
            )

        path = artifact_path or os.environ.get("MODEL_PATH")

        if path:
            model, metadata = load(path)
        else:
            model = None
            metadata = {
                "version": "openlibrary-content-v1",
                "domain": "books",
                "namespace": "openlibrary-work",
                "algorithm": "content",
            }

        app.state.key = key
        app.state.model = model
        app.state.metadata = metadata

        async with httpx.AsyncClient(
            timeout=6,
            transport=upstream_transport,
            limits=httpx.Limits(max_connections=10),
        ) as client:
            app.state.home = HomeRecommender(
                client,
                model,
                metadata,
                os.environ.get(
                    "PROFILE_SERVICE_URL",
                    "http://localhost:8082",
                ),
                os.environ.get(
                    "BOOKS_SERVICE_URL",
                    "http://localhost:8081",
                ),
            )

            worker = BookCatalogWorker().start()
            music_worker = MusicCatalogWorker().start()
            try:
                yield
            finally:
                worker.stop()
                music_worker.stop()

    app = FastAPI(
        title="musa recommendations",
        lifespan=lifespan,
    )
    bearer = HTTPBearer(auto_error=False)

    def authenticate(
        credentials: HTTPAuthorizationCredentials | None = Depends(
            bearer
        ),
    ):
        try:
            if (
                not credentials
                or credentials.scheme.lower() != "bearer"
            ):
                raise ValueError("missing bearer")

            claims = jwt.decode(
                credentials.credentials,
                app.state.key,
                algorithms=["HS512"],
                issuer="musa-auth",
                audience="musa-api",
                options={
                    "require": ["sub", "exp", "iss", "aud"],
                },
            )

            if str(UUID(claims["sub"])) != claims["sub"].lower():
                raise ValueError("invalid subject")

            return claims["sub"]

        except (
            jwt.PyJWTError,
            ValueError,
            TypeError,
            AttributeError,
        ):
            raise HTTPException(
                status_code=401,
                detail="Token inválido o expirado",
                headers={"WWW-Authenticate": "Bearer"},
            ) from None

    @app.get("/health/live")
    def live():
        return {"status": "ok"}

    @app.get("/health/ready")
    def ready():
        return {
            "status": "ready",
            "version": app.state.metadata["version"],
        }

    @app.get("/api/recommendations/books/me")
    async def home_books(
        response: Response,
        limit: int = Query(default=12, ge=1, le=20),
        subject=Depends(authenticate),
        credentials=Depends(bearer),
    ):
        response.headers["Cache-Control"] = "no-store"
        response.headers["Vary"] = "Authorization"

        try:
            async with asyncio.timeout(18):
                return await app.state.home.recommend(
                    credentials.credentials,
                    limit,
                )

        except TimeoutError:
            raise HTTPException(
                status_code=503,
                detail=(
                    "Las recomendaciones tardaron demasiado. "
                    "Intenta nuevamente."
                ),
            ) from None

    @app.post(
        "/api/recommendations/books/{external_id}/features",
        response_model=BookFeatures,
    )
    def book_features(
        external_id: str,
        subject=Depends(authenticate),
        credentials=Depends(bearer),
    ):
        work_id = normalize_work_id(external_id)

        if work_id is None:
            raise HTTPException(
                status_code=422,
                detail="Identificador de OpenLibrary inválido",
            )

        try:
            return get_features_for_book(
                work_id,
                credentials.credentials,
            )

        except httpx.HTTPStatusError as error:
            upstream_status = error.response.status_code

            if upstream_status == 429:
                raise HTTPException(
                    status_code=503,
                    detail=(
                        "Se alcanzó el límite del proveedor. "
                        "Intenta más tarde."
                    ),
                ) from None

            raise HTTPException(
                status_code=502,
                detail=(
                    "No se pudo completar la consulta "
                    "a un servicio externo."
                ),
            ) from None

        except httpx.RequestError:
            raise HTTPException(
                status_code=503,
                detail="Un servicio necesario no está disponible.",
            ) from None

        except psycopg.Error:
            raise HTTPException(
                status_code=503,
                detail=(
                    "No se pudo acceder a las "
                    "características guardadas."
                ),
            ) from None

        except PreparationDeferred as error:
            raise HTTPException(status_code=429, detail="La preparación está temporalmente limitada. Intenta más tarde.",
                                headers={"Retry-After": str(error.seconds)}) from None
        except ValueError:
            raise HTTPException(
                status_code=502,
                detail=(
                    "No se pudieron obtener "
                    "características válidas."
                ),
            ) from None

    @app.post("/api/recommendations/{domain}")
    def recommendations(
        domain: str,
        request: RecommendationRequest,
        subject=Depends(authenticate),
    ):
        metadata = app.state.metadata

        if domain != metadata["domain"]:
            raise HTTPException(
                status_code=404,
                detail="Modelo no disponible para este dominio",
            )

        model = app.state.model

        if model is None:
            raise HTTPException(
                status_code=503,
                detail=(
                    "Usa el endpoint de Inicio o configura "
                    "un modelo entrenado"
                ),
            )

        if metadata.get("serving_namespace") == "openlibrary-work":
            if not model.external_ids:
                raise HTTPException(
                    status_code=503,
                    detail="No hay equivalencias OpenLibrary verificadas",
                )

            if any(
                normalize_work_id(value) is None
                for value in request.seeds + request.exclude
            ):
                raise HTTPException(
                    status_code=422,
                    detail="Se requieren IDs de obra OpenLibrary OL...W",
                )

            seeds = {
                normalize_work_id(value)
                for value in request.seeds
            }
            exclude = {
                normalize_work_id(value)
                for value in request.exclude
            }

            reverse = {}

            for internal, external in model.external_ids.items():
                reverse.setdefault(external, []).append(internal)

            internal_seeds = [
                internal
                for external in seeds
                for internal in reverse.get(external, [])
            ]
            internal_exclude = [
                internal
                for external in exclude
                for internal in reverse.get(external, [])
            ]

            ranked = model.recommend(
                internal_seeds,
                internal_exclude,
                len(model.ids),
                metadata["algorithm"],
                allowed=model.external_ids,
            )

            items = []
            used = set()

            for row in ranked:
                external = model.external_ids[row["item_id"]]

                if external in used:
                    continue

                used.add(external)

                items.append(
                    {
                        **row,
                        "goodbooks_id": row["item_id"],
                        "item_id": external,
                        "externalId": external,
                    }
                )

                if len(items) == request.limit:
                    break

            return {
                "model_version": metadata["version"],
                "namespace": "openlibrary-work",
                "model_namespace": metadata["namespace"],
                "mapped_items": len(model.external_ids),
                "total_items": len(model.ids),
                "unmapped_seeds": sorted(seeds - set(reverse)),
                "items": items,
            }

        unknown = set(request.seeds) - set(model.ids)

        if unknown:
            raise HTTPException(
                status_code=422,
                detail="Semillas fuera del catálogo/namespace del modelo",
            )

        return {
            "model_version": metadata["version"],
            "namespace": metadata["namespace"],
            "items": model.recommend(
                request.seeds,
                request.exclude,
                request.limit,
                metadata["algorithm"],
            ),
        }

    @app.get("/api/recommendations/books/{external_id}/similar")
    def similar_books(
        external_id: str,
        response: Response,
        limit: int = Query(default=8, ge=1, le=20),
        subject=Depends(authenticate),
        credentials=Depends(bearer),
    ):
        response.headers["Cache-Control"] = "no-store"
        work_id = normalize_work_id(external_id)
        if work_id is None:
            raise HTTPException(status_code=422, detail="Identificador de OpenLibrary inválido")
        try:
            return recommend_similar_books(work_id, limit, credentials.credentials)
        except psycopg.Error:
            raise HTTPException(status_code=503, detail="No se pudo consultar el catálogo de libros") from None
        except ValueError:
            raise HTTPException(status_code=502, detail="Se recibieron datos de recomendaciones inválidos") from None

    @app.get("/api/recommendations/books/knn/me")
    def book_knn_recommendations(
        response: Response,
        limit: int = Query(default=12, ge=1, le=20),
        subject=Depends(authenticate),
        credentials=Depends(bearer),
    ):
        response.headers["Cache-Control"] = "no-store"
        response.headers["Vary"] = "Authorization"
        try:
            return recommend_books_for_user(credentials.credentials, limit)
        except httpx.HTTPStatusError as error:
            status = error.response.status_code
            if status in (401, 403):
                raise HTTPException(
                    status_code=status,
                    detail="Tu sesión no permite consultar tus gustos",
                ) from None
            raise HTTPException(
                status_code=503,
                detail="No se pudieron consultar tus gustos",
            ) from None
        except (httpx.RequestError, psycopg.Error):
            raise HTTPException(
                status_code=503,
                detail="Un servicio necesario no está disponible",
            ) from None
        except ValueError:
            raise HTTPException(
                status_code=502,
                detail="Se recibieron datos de recomendaciones inválidos",
            ) from None


    @app.get("/api/recommendations/music/{kind}/{external_id}/similar")
    def similar_music(
        response: Response,
        kind: Literal["songs", "albums", "artists"],
        external_id: str = Path(min_length=1, max_length=255, pattern=r"^[A-Za-z0-9_-]+$"),
        limit: int = Query(default=8, ge=1, le=20),
        subject=Depends(authenticate),
    ):
        response.headers["Cache-Control"] = "no-store"
        response.headers["Vary"] = "Authorization"
        try:
            return recommend_similar_music(MUSIC_KINDS[kind],external_id,limit)
        except psycopg.Error:
            raise HTTPException(status_code=503,detail="No se pudo consultar el catálogo musical") from None
        except ValueError:
            raise HTTPException(status_code=502,detail="Se recibieron datos musicales inválidos") from None

    @app.get("/api/recommendations/music/{kind}/me")
    def music_recommendations(
        kind: Literal["songs", "albums", "artists"],
        response: Response,
        limit: int = Query(default=12, ge=1, le=20),
        subject=Depends(authenticate),
        credentials=Depends(bearer),
    ):
        response.headers["Cache-Control"] = "no-store"
        response.headers["Vary"] = "Authorization"
        try:
            return recommend_music_for_user(credentials.credentials, MUSIC_KINDS[kind], limit)
        except httpx.HTTPStatusError as error:
            status = error.response.status_code
            if status in (401, 403):
                raise HTTPException(status_code=status, detail="Tu sesión no permite consultar tus gustos") from None
            raise HTTPException(status_code=503, detail="No se pudieron consultar tus gustos musicales") from None
        except (httpx.RequestError, psycopg.Error):
            raise HTTPException(status_code=503, detail="Un servicio necesario no está disponible") from None
        except ValueError:
            raise HTTPException(status_code=502, detail="Se recibieron datos musicales inválidos") from None

    return app


app = create_app()