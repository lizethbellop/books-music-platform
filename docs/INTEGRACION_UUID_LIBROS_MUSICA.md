# UUID y autenticación de Libros y Música

## Comportamiento implementado

- Usuarios emite el JWT con el UUID de la cuenta en `sub`, emisor `musa-auth`, audiencia `musa-api` y firma HS512.
- Libros y Música validan la firma, emisor, audiencia, vencimiento y formato UUID antes de aceptar peticiones.
- Las rutas de `/books/**` y `/api/music/**` requieren `Authorization: Bearer <accessToken>`. El health de Música y las peticiones OPTIONS de CORS son excepciones.
- Cada servicio mantiene su propia BD. Validar el token no implica consultar la BD de Usuarios.
- Los parámetros heredados `userId` nunca acreditan identidad: deben coincidir con `sub`. Si no coinciden, la respuesta es 403 y la operación no se ejecuta.
- En Libros, `userId` en query es opcional; sin él se utiliza el UUID del token. En Música, las rutas con UUID y los DTO existentes se conservan para no romper los clientes; la eliminación de favoritos admite omitir el parámetro query.
- La edición y eliminación de una reseña musical comprueban que pertenezca al UUID autenticado. Una reseña inexistente devuelve 404.
- Flutter usa `AuthenticatedApiClient` para enviar el token actual en todas las peticiones de Libros y Música. Consulta `AuthSessionManager.getAccessToken()`, que utiliza la renovación ya implementada cuando corresponde.
- Las pantallas de Libros reciben el UUID real desde AppShell, incluida la pantalla de detalle. Se eliminó el usuario de prueba fijo de esas pantallas.

## Configuración local

Cada `.env` de Usuarios, Perfil, Libros y Música necesita el mismo `JWT_SECRET` local. Se utiliza la clave ya configurada en Usuarios; no generar otra distinta para cada servicio. El valor debe ser Base64 de al menos 64 bytes.

Los `.env` están ignorados por Git. Los `.env.example` contienen únicamente `JWT_SECRET=` sin el valor. Las credenciales de Spotify y DeepL se mantienen en sus servicios; Flutter no las recibe.

Libros ya tenía las columnas `user_id` de tipo UUID. Música ya tenía aplicada localmente la migración documentada en `MIGRACION_UUID_MUSICA.md`. Este cambio no transforma identificadores de contenido: los ID musicales locales siguen siendo numéricos, `spotifyId` sigue siendo texto y los identificadores locales de libros conservan su tipo existente.

Después de estos cambios, reiniciar los procesos de Libros y Música que estuvieran abiertos para que carguen la configuración nueva. Puertos habituales: Usuarios 8083, Perfil 8082, Libros 8081 y Música 8080.

## Pruebas

Desde cada carpeta `backend/music-service` o `backend/books-service`:

```sh
# Tokens, endpoints y permisos con servicios simulados; no necesita BD.
bash ./mvnw -DexcludedGroups=integration test

# Incluye PostgreSQL local y los .env del servicio.
bash ./mvnw test
```

Las pruebas HTTP con PostgreSQL recorren las acciones de dos usuarios distintos y usan transacciones que revierten sus registros al terminar. La configuración de esas pruebas usa `ddl-auto=validate` para no crear ni modificar tablas. Los tests existentes de contexto conservan su comportamiento y se identifican como `integration`.

Desde `frontend`:

```sh
flutter analyze
flutter test
flutter build web
```

Las nuevas pruebas de Flutter verifican el encabezado Bearer, el UUID enviado a Libros y Música, la conservación del JSON, la consulta del token en cada petición y que no se envíen peticiones cuando no existe una sesión disponible.

## Resultados automatizados

- Música: 25 casos correctos en total (22 sin BD y 3 de integración).
- Libros: 17 casos correctos en total (15 sin BD y 2 de integración).
- Flutter: 8 pruebas correctas; `flutter analyze` sin incidencias; compilación web completada.

## Verificación local realizada

- Música: validación de JWT reales, seguridad HTTP, permisos de reseñas y persistencia separada de UUID en PostgreSQL.
- Libros: validación de JWT reales, seguridad HTTP y biblioteca, favoritos, calificaciones y reseñas de dos usuarios en PostgreSQL.
- Búsquedas autenticadas mediante los backends: Spotify y Open Library devolvieron 200 y resultados. El endpoint de prueba de Spotify obtuvo un token correctamente.
- DeepL devolvió 200 en la consulta de uso de la cuenta. Esto comprueba la credencial; no sustituye la prueba manual de traducción de un libro.
- Un request sin token fue rechazado con 401 en ambos backends.
- Las claves locales coinciden entre los cuatro servicios y sus valores no se imprimieron.

Estas comprobaciones no sustituyen el recorrido manual de la aplicación ni prueban las compilaciones nativas de Android, iOS, Windows o macOS.

## Lo que sigue

Conectar las listas de Perfil al contenido de Libros y Música mediante los contratos REST, conservando la independencia de las BD. Después realizar el recorrido manual completo de login, renovación de sesión, perfil y acciones de contenido. API Gateway permanece para el siguiente sprint.

No se hicieron commits ni se subieron cambios a GitHub.
