"""Comprobación manual con una sesión existente; la app utiliza api.py."""
import argparse
from getpass import getpass

from .service.book_features_service import get_features_for_book


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--id", default="OL26350090W")
    args = parser.parse_args()
    token = getpass("Token de sesión vigente, sin Bearer: ").strip()
    features = get_features_for_book(args.id, token)
    print(features.model_dump_json(indent=2))


if __name__ == "__main__":
    main()
