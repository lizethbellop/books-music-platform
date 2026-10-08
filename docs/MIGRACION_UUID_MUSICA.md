# Usuarios de música con UUID

Música usa ahora el UUID del usuario de autenticación en favoritos, calificaciones y reseñas. Los IDs internos del contenido y de las reseñas siguen siendo números; spotifyId sigue siendo texto.

## BD existente

1. Detener música y respaldar music_db con pg_dump.
2. Si hay acciones de usuarios, identificar con certeza qué UUID de autenticación corresponde a cada antiguo número. Añadir esas correspondencias en database/migrations/001_user_id_uuid.sql. No convertir IDs ni inventar UUIDs.
3. Desde backend/music-service ejecutar:

```sh
psql -h localhost -U music_user -d music_db -v ON_ERROR_STOP=1 -f database/migrations/001_user_id_uuid.sql
```

La migración es una transacción y se detiene sin cambios si falta una correspondencia. Puede ejecutarse nuevamente después de completarla. Para una BD nueva, schema.sql ya utiliza UUID.

## Flutter y autenticación

AppShell pasa su userId a música y al detalle; ya no se envía el usuario numérico 1. El cliente de música recibe UUID como String.

Pendiente en la siguiente etapa: conectar la sesión real de login a AppShell y obtener la identidad del token validado en el backend. Cambiar el tipo de identificador todavía no implementa autenticación ni permisos. El UUID de desarrollo de AppShell sigue disponible para las pruebas locales.
