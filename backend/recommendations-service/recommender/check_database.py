from .database import get_connection


def main():
    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT current_database(), current_user"
            )
            database, user = cursor.fetchone()

            print(f"Base conectada: {database}")
            print(f"Usuario conectado: {user}")

            cursor.execute(
                "SELECT COUNT(*) FROM public.book_features"
            )
            count = cursor.fetchone()[0]

            print(f"Libros con características guardadas: {count}")


if __name__ == "__main__":
    main()