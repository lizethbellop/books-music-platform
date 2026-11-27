import os
from pathlib import Path

import psycopg
from dotenv import load_dotenv


ENV_PATH = Path(__file__).resolve().parent.parent / ".env"


def get_connection():
    load_dotenv(ENV_PATH, override=False)

    return psycopg.connect(
        host=os.environ["RECOMMENDATIONS_DB_HOST"],
        port=int(os.environ["RECOMMENDATIONS_DB_PORT"]),
        dbname=os.environ["RECOMMENDATIONS_DB_NAME"],
        user=os.environ["RECOMMENDATIONS_DB_USER"],
        password=os.environ["RECOMMENDATIONS_DB_PASSWORD"],
        connect_timeout=5,
    )