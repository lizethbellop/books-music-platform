"""Start the service with musa's existing JWT key, without copying secrets into code."""
import argparse
import base64
import os
from pathlib import Path

from dotenv import dotenv_values, load_dotenv
import uvicorn


def configure(env_file=".env", auth_env=None):
    load_dotenv(env_file, override=False)
    if not os.environ.get("JWT_SECRET") and auth_env:
        path = Path(auth_env)
        if not path.is_file():
            raise ValueError("No existe el archivo de configuración de autenticación indicado")
        secret = dotenv_values(path).get("JWT_SECRET")
        if secret:
            # Import only the signing key, never the Books database/provider credentials.
            os.environ["JWT_SECRET"] = secret
    secret = os.environ.get("JWT_SECRET", "")
    try:
        key = base64.b64decode(secret, validate=True)
    except ValueError:
        raise ValueError("JWT_SECRET debe ser una clave Base64 válida") from None
    if len(key) < 64:
        raise ValueError("Configura la misma JWT_SECRET de musa. (al menos 64 bytes decodificados)")
    path = os.environ.get("MODEL_PATH")
    if path and not Path(path).is_dir():
        raise ValueError("MODEL_PATH no existe: copia el artefacto o deja la variable vacía para usar contenido")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--env-file", default=".env")
    parser.add_argument("--auth-env", help="Books/users .env; reads only JWT_SECRET if not already configured")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8087)
    args = parser.parse_args()
    try:
        configure(args.env_file, args.auth_env)
    except ValueError as error:
        parser.error(str(error))
    uvicorn.run("recommender.api:app", host=args.host, port=args.port)


if __name__ == "__main__":
    main()
