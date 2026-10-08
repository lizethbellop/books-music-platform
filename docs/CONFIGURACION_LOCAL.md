Para el recorrido paso a paso: [Windows](GUIA_WINDOWS.md), [Mac](GUIA_MAC.md) y [campos de .env / pruebas manuales](GUIA_LOCAL.md).

# Configuración local y secretos

Desde la raíz, ejecutar `python3 scripts/setup-local-env.py` (Windows: `py scripts/setup-local-env.py`). Crea un `.env` ignorado por Git en cada servicio; conserva los archivos existentes y genera una clave JWT Base64 aleatoria para autenticación. Completar las contraseñas y credenciales de cada integrante. Se suben los `.env.example`, nunca los `.env`.

Los archivos `.env` usan formato Java properties: `NOMBRE=valor`, sin `export`, `$env:` ni comillas alrededor del valor. No pegar contraseñas en el código ni en Flutter. Las variables de entorno tienen prioridad sobre estos archivos. Para JWT usar una clave Base64 de al menos 64 bytes; el script genera 64 bytes. La clave local debe permanecer estable entre reinicios para conservar la validez de los tokens existentes.

Iniciar cada backend **desde su propia carpeta**, con `bash mvnw spring-boot:run` en macOS/Linux o `.\mvnw.cmd spring-boot:run` en PowerShell. Spring carga automáticamente el `.env` de esa carpeta. PostgreSQL y las bases independientes deben estar disponibles; autenticación también usa Redis. Se conserva la preparación del esquema de perfil documentada en el servicio.

| Servicio | Puerto | Credenciales locales |
| --- | --- | --- |
| Música | 8080 | PostgreSQL, Spotify Client ID y Client Secret |
| Libros | 8081 | PostgreSQL, DeepL |
| Perfil | 8082 | PostgreSQL, Cloudinary para subir fotos |
| Usuarios | 8083 | PostgreSQL, JWT, SMTP; Redis en 6379 |

En `frontend`, ejecutar `flutter run -d chrome --web-port 3000` (Windows: `-d edge`). Si autenticación ya corre en otro puerto, usar `--dart-define=AUTH_API_URL=http://localhost:PUERTO/api/v1/auth`. También siguen disponibles `MUSIC_API_URL`, `BOOKS_API_URL` y `PROFILE_API_URL`.

## Base nueva de usuarios

En una instalación nueva, ejecutar `backend/users-service/database/schema.sql` y después `backend/users-service/database/seed.sql` en la BD de usuarios para cargar los roles `USUARIO` y `VENDEDOR` del formulario. El archivo conserva los roles que ya existan. En una base anterior sin username, aplicar primero `backend/users-service/database/migrations/001_username.sql` antes de encender Usuarios. Para Windows, seguir `docs/GUIA_WINDOWS.md`. No reutilizar una base antigua con un esquema diferente. El registro actual envía un correo de bienvenida; requiere las credenciales SMTP configuradas por la responsable de usuarios.

## GitGuardian

El incidente 37851969 señala una cadena fija de prueba en `backend/users-service/src/test/resources/application.properties`. Se eliminó ese valor; las pruebas ahora generan la clave al arrancar y comprueban que firma y valida JWT. Producción continúa exigiendo `JWT_SECRET`; no recibe la clave temporal de pruebas. No se deshabilita ni se ignora GitGuardian.

El commit histórico permanece en Git. Subir el arreglo no elimina automáticamente el incidente del panel: revisar la alerta y clasificarla como credencial ficticia de prueba si ese valor nunca se usó fuera de pruebas. Si se reutilizó como credencial real, reemplazarla también donde se haya usado e invalidar sus tokens. No se ha reescrito el historial del equipo.

Se retiraron las copias compiladas del directorio `backend/books-service/bin` del conjunto versionado. Los fuentes, esquema, configuración y Maven originales permanecen en `backend/books-service`; `bin` queda ignorado.

El acceso de navegador a los servicios de música, libros y perfil admite `localhost` y `127.0.0.1` con puertos locales variables. Esto permite usar el puerto fijo 3000 o el que Flutter asigne durante desarrollo.

## Prueba manual antes de subir

1. Reiniciar completamente Flutter y los cuatro servicios; actualizar la página del navegador.
2. Iniciar sesión/registrarse con el servicio de usuarios en 8083.
3. Desde Inicio abrir Música, Libros y Mi perfil. Debe aparecer una sola barra lateral clara en escritorio.
4. Reducir el ancho de la ventana: comprobar navegación inferior y menú claro para Social, Comunidades y Estadísticas (pendientes).
5. Música: buscar, abrir canción/álbum, calificar, crear/editar/eliminar reseña, agregar y quitar favorito.
6. Libros: buscar, abrir detalle, cambiar estado de lectura, calificar, reseñar y comprobar favoritos/biblioteca.
7. Perfil: consultar, editar, cambiar foto, preferencias y crear/editar/eliminar listas.
8. Confirmar que `git status --short` no muestra `.env`, claves ni credenciales. Subir juntos los archivos de navegación, rutas y configuración; ejecutar las comprobaciones de CI.

La vinculación de la sesión autenticada con los identificadores de desarrollo de libros/perfil y el identificador numérico de música sigue pendiente en el código actual; este arreglo conserva sus contratos existentes.
