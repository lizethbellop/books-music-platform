# Prueba local: autenticación, perfil y listas

## Resultado de la revisión del 8 de octubre de 2026

Se verificó por HTTP real: registro, login, perfil inexistente 404, inicialización idempotente, consulta de perfil y preferencias, búsqueda mediante Libros/Open Library y Música/Spotify, creación de lista, agregado y resolución de libro/canción/artista, duplicado 409, eliminación de lista, renovación y revocación de sesión. La renovación conserva exactamente sessionExpiresAt; el refresh anterior y el cerrado devuelven 401. Las cuentas, perfiles y listas temporales de esta prueba se eliminaron.

Perfil consulta los catálogos exclusivamente mediante REST y propaga el Bearer del usuario. No consulta sus bases de datos ni sus proveedores externos. Las listas almacenan referencias y resuelven nombres e imágenes mediante los servicios propietarios. Un contenido inaccesible conserva su referencia con UNAVAILABLE.

Se corrigieron el UUID usado al abrir la aplicación, la recuperación de sesión al arrancar, preferencias con identidad JWT, el retorno al login y el botón de cierre de sesión en Perfil. El avatar circular inferior se conserva. Los despachos internos ERROR de Spring pueden producir sus códigos originales; una llamada directa a una ruta no autorizada continúa protegida. El vencimiento se expresa con precisión de milisegundos tanto en login como en Redis.

Verificaciones: pruebas de los cuatro servicios, listas con PostgreSQL y concurrencia, sesiones con Redis, Flutter analyze y Flutter test, compilación web y compilación macOS debug. Las compilaciones no sustituyen la revisión manual de pantallas. Android, iOS y Windows no se ejecutaron en esta revisión.

## Encender el entorno

Postgres.app debe estar encendido y Redis debe escuchar en localhost:6379. Cada backend usa su propia BD y su propio archivo .env local. No subir esos archivos; las plantillas .env.example sirven para preparar otras computadoras.

En cuatro terminales independientes, ejecutar el bloque del servicio correspondiente. Si ya están encendidos, no iniciar otra copia.

### Usuarios, puerto 8083
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/users-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash ./mvnw spring-boot:run
```
### Perfil, puerto 8082
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/profile-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash ./mvnw spring-boot:run
```
### Libros, puerto 8081
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/books-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash ./mvnw spring-boot:run
```
### Música, puerto 8080
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/music-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash ./mvnw spring-boot:run
```

Esperar a que cada terminal muestre Started. Para detener el servicio, Ctrl+C en su terminal.

### Flutter en Mac
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/frontend"
flutter run -d macos
```
### Flutter web, como alternativa
```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/frontend"
flutter run -d chrome --web-port=5000
```
Usar siempre el mismo puerto web al comprobar Recordarme: el almacenamiento depende del origen del navegador.

## Recorrido manual

1. Registrar una cuenta propia y entrar. El nombre y la sesión deben corresponder a esa cuenta.
2. Abrir Perfil: se crea automáticamente si todavía no existe. Verificar el nombre de usuario, el único sidebar claro y el avatar circular inferior.
3. Crear una lista. Abrirla y seleccionar Agregar; buscar y agregar un libro, una canción y un artista. Comprobar título, autor/artista e imagen cuando el catálogo la incluya.
4. Cerrar y volver a abrir la lista. Deben conservarse los tres elementos. Intentar agregar un duplicado: debe informarse sin duplicarlo.
5. Quitar un elemento y volver a abrir la lista: debe permanecer eliminado.
6. Revisar las pantallas de Libros y Música y sus acciones habituales (detalle, favoritos, calificación y reseñas) con la cuenta iniciada.
7. En Perfil pulsar Cerrar sesión: debe volver al login. Entrar con otra cuenta: no debe ver las listas privadas de la primera.
8. Sin Recordarme, cerrar completamente la aplicación y abrirla: debe pedir login. Con Recordarme, repetir: debe recuperar la sesión. El access token dura 15 minutos; se renueva al pedir acceso cerca del vencimiento, hasta 7 días desde el login. Renovar no reinicia ese plazo.
9. Para comprobar renovación automática, mantener la app abierta más de 15 minutos y abrir una pantalla que consulte un servicio.

## Alcance y entrega

Esta revisión cubre la integración actual de autenticación, perfil, libros, música y listas. Los módulos marcados pendiente en la interfaz continúan pendientes; la pantalla de recuperación de contraseña aún no completa el flujo del backend. No se afirma que todos los contratos tengan errores uniformes: esa normalización sigue indicada como pendiente.

Antes de publicar, terminar este recorrido manual y revisar los archivos que se van a incluir. La alerta histórica de GitGuardian no desaparece por retirar un secreto del archivo actual: los valores expuestos deben revocarse o rotarse y el incidente debe revisarse. No se reescribió historial ni se publicó nada en GitHub durante esta revisión.
