-- Ejecutar con psql como postgres, conectado a postgres.
-- Este archivo utiliza comandos de psql (\gexec), no el editor SQL de pgAdmin.
-- Solo crea roles/bases ausentes; no borra datos ni cambia propietarios existentes.
SELECT format('CREATE ROLE %I LOGIN', name)
FROM (VALUES ('auth_user'), ('profile_user'), ('books_user'), ('music_user')) AS wanted(name)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = wanted.name)
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', name, owner)
FROM (VALUES ('BDAuth', 'auth_user'), ('profile_db', 'profile_user'),
             ('books_db', 'books_user'), ('music_db', 'music_user')) AS wanted(name, owner)
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = wanted.name)
\gexec
