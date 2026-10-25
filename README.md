# Books & Music Platform

Plataforma de libros y música con frontend Flutter y backend de microservicios.

El frontend se comunica con el API Gateway en el puerto **8084**. El Gateway dirige cada solicitud al microservicio correspondiente, conservando las rutas y los tokens JWT.

Para preparar los archivos locales de secretos, consultar [Configuración local](docs/CONFIGURACION_LOCAL.md).

Para preparar el entorno, seguir la guía de [Windows](docs/GUIA_WINDOWS.md) o [Mac](docs/GUIA_MAC.md). Los [valores de configuración y recorrido manual](docs/GUIA_LOCAL.md) se explican paso a paso. El [inventario de pruebas](docs/CONTEO_PRUEBAS.md) distingue unitarias, pruebas Spring e integración.

## Puertos locales

Mantener esta asignación en los entornos locales del equipo:

| Aplicación o servicio | Puerto | URL base |
| --- | ---: | --- |
| Frontend Flutter web | 3000 | `http://localhost:3000` |
| Music Service | 8080 | `http://localhost:8080/api/music` |
| Books Service | 8081 | `http://localhost:8081/books` |
| Profile Service | 8082 | `http://localhost:8082/api/profiles` |
| Users Service | 8083 | `http://localhost:8083` |
| API Gateway | 8084 | `http://localhost:8084` |
| Social Service (reservado) | 8085 | `http://localhost:8085` |
| Communities Service (reservado) | 8086 | `http://localhost:8086` |
| Chat Service (reservado) | 8087 | `http://localhost:8087` |
| Recommendations Service (reservado) | 8088 | `http://localhost:8088` |

Una misma instancia de PostgreSQL puede atender las bases de datos de los servicios en el puerto `5432`.

## Antes de iniciar

1. Iniciar PostgreSQL.
2. Confirmar que existan las bases de datos y los usuarios requeridos.
3. Configurar las contraseñas y credenciales externas según las guías locales. No subir secretos a GitHub.
4. Iniciar Users, Music, Books y Profile en terminales separadas.
5. Iniciar el API Gateway.
6. Iniciar Flutter.

Los servicios reservados no son necesarios para los flujos actuales.

## Iniciar en Windows PowerShell

Los ejemplos suponen que el repositorio está en `D:\books-music-platform`. Ajustar la ruta según la ubicación local.

### Users Service — puerto 8083

Configurar primero las variables de entorno indicadas en [Configuración local](docs/CONFIGURACION_LOCAL.md).

```powershell
cd D:\books-music-platform\backend\users-service
.\mvnw.cmd spring-boot:run
```

### Music Service — puerto 8080

```powershell
cd D:\books-music-platform\backend\music-service
$env:MUSIC_DB_PASSWORD="TU_PASSWORD"
$env:SPOTIFY_CLIENT_ID="TU_CLIENT_ID"
$env:SPOTIFY_CLIENT_SECRET="TU_CLIENT_SECRET"
.\mvnw.cmd spring-boot:run
```

### Books Service — puerto 8081

```powershell
cd D:\books-music-platform\backend\books-service
$env:BOOKS_DB_PASSWORD="TU_PASSWORD"
.\mvnw.cmd spring-boot:run
```

### Profile Service — puerto 8082

Usar los valores locales de PostgreSQL y Cloudinary:

```powershell
cd D:\books-music-platform\backend\profile-service
$env:PROFILE_DB_URL="jdbc:postgresql://localhost:5432/profile_db"
$env:PROFILE_DB_USERNAME="profile_user"
$env:PROFILE_DB_PASSWORD="TU_PASSWORD"
$env:CLOUDINARY_URL="TU_CLOUDINARY_URL"
.\mvnw.cmd spring-boot:run
```

### API Gateway — puerto 8084

```powershell
cd D:\books-music-platform\backend\api-gateway
.\mvnw.cmd spring-boot:run
```

### Frontend Flutter web — puerto 3000

```powershell
cd D:\books-music-platform\frontend
flutter run -d edge --web-port 3000
```

Si Flutter no detecta el navegador:

```powershell
flutter run -d web-server --web-port 3000
```

Abrir manualmente `http://localhost:3000` y mantener la terminal abierta.

## Iniciar en macOS o Linux

Abrir una terminal por servicio. Cada bloque comienza desde la raíz del repositorio.

### Users Service — puerto 8083

Configurar primero las variables de entorno indicadas en [Configuración local](docs/CONFIGURACION_LOCAL.md).

```bash
cd backend/users-service
bash mvnw spring-boot:run
```

### Music Service — puerto 8080

```bash
cd backend/music-service
MUSIC_DB_PASSWORD='TU_PASSWORD' \
SPOTIFY_CLIENT_ID='TU_CLIENT_ID' \
SPOTIFY_CLIENT_SECRET='TU_CLIENT_SECRET' \
bash mvnw spring-boot:run
```

### Books Service — puerto 8081

```bash
cd backend/books-service
BOOKS_DB_PASSWORD='TU_PASSWORD' bash mvnw spring-boot:run
```

### Profile Service — puerto 8082

```bash
cd backend/profile-service
PROFILE_DB_URL='jdbc:postgresql://localhost:5432/profile_db' \
PROFILE_DB_USERNAME='profile_user' \
PROFILE_DB_PASSWORD='TU_PASSWORD' \
CLOUDINARY_URL='TU_CLOUDINARY_URL' \
bash mvnw spring-boot:run
```

### API Gateway — puerto 8084

```bash
cd backend/api-gateway
bash mvnw spring-boot:run
```

### Frontend Flutter web — puerto 3000

Con Chrome instalado y detectado por Flutter:

```bash
cd frontend
flutter run -d chrome --web-port 3000
```

Si Chrome no está disponible, usar:

```bash
cd frontend
flutter run -d web-server --web-port 3000
```

Abrir manualmente `http://localhost:3000` en Safari u otro navegador. El modo `web-server` no abre el navegador automáticamente. Mantener la terminal abierta mientras se utiliza la aplicación.

## Configuración del frontend

Las URLs del backend están centralizadas en:

`frontend/lib/core/config/api_config.dart`

La configuración predeterminada es:

```dart
abstract final class ApiConfig {
  static const String gatewayUrl = String.fromEnvironment(
    'GATEWAY_URL',
    defaultValue: 'http://localhost:8084',
  );

  static const String authUrl = '$gatewayUrl/api/v1/auth';
  static const String booksUrl = '$gatewayUrl/books';
  static const String musicUrl = '$gatewayUrl/api/music';
  static const String profileUrl = '$gatewayUrl/api/profiles';
  static const String usernamesUrl = '$gatewayUrl/api/v1/users/usernames';
}
```

Para cambiar la dirección del Gateway:

```bash
flutter run -d web-server --web-port 3000 --dart-define=GATEWAY_URL=http://localhost:8084
```

`GATEWAY_URL` debe ser un origen sin barra final ni rutas. Reiniciar la ejecución de Flutter al cambiar este valor.

Las opciones anteriores `AUTH_API_URL`, `BOOKS_API_URL`, `MUSIC_API_URL` y `PROFILE_API_URL` ya no configuran el frontend.

Para el emulador Android, usar `http://10.0.2.2:8084`. En un dispositivo físico, usar la dirección de red del equipo donde se ejecuta el Gateway.

## Routing del Gateway

El Gateway debe conservar las rutas completas al reenviar las solicitudes:

| Ruta de entrada | Servicio de destino |
| --- | --- |
| `/api/v1/auth/**` | Users Service — 8083 |
| `/api/v1/users/**` | Users Service — 8083 |
| `/books/**` | Books Service — 8081 |
| `/api/music/**` | Music Service — 8080 |
| `/api/profiles/**` | Profile Service — 8082 |

Los predicates son las condiciones que determinan qué ruta debe atender una solicitud.

El Gateway debe conservar consultas, cuerpos y el encabezado `Authorization`. También debe permitir CORS desde el origen del frontend, incluidas las solicitudes `OPTIONS`.

## Autenticación y JWT

Users Service emite los tokens de autenticación.

El frontend utiliza el access token en las solicitudes protegidas:

```text
Authorization: Bearer <accessToken>
```

El gestor de sesión conserva el mecanismo de renovación mediante refresh token. El Gateway reenvía el encabezado de autorización y los microservicios mantienen su validación de JWT y permisos.

La integración del Gateway conserva los contratos, los UUID, los métodos HTTP y los formatos de las solicitudes, incluida la subida de fotos.

## Pruebas del frontend

Desde la raíz del repositorio:

```bash
cd frontend
flutter analyze
flutter test
```

Estas comprobaciones pueden ejecutarse con los servicios apagados. Las pruebas con clientes simulados no sustituyen la comprobación del sistema completo.

Para comprobar una dirección alternativa del Gateway:

```bash
flutter test --dart-define=GATEWAY_URL=http://127.0.0.1:8084 test/api_config_test.dart test/authenticated_catalog_api_test.dart test/review_usernames_test.dart test/auth_api_error_test.dart
```

### Comprobación manual

Con PostgreSQL, los cuatro servicios, el Gateway y Flutter iniciados:

1. Iniciar sesión.
2. Buscar libros y música.
3. Consultar detalles, favoritos y reseñas.
4. Verificar los nombres de autores de reseñas.
5. Consultar y editar el perfil.
6. Probar la foto, las preferencias y las listas.
7. Cerrar sesión.

En las herramientas del navegador, abrir **Network/Red** y verificar que las solicitudes a las APIs del backend utilizan `http://localhost:8084`.

Las imágenes y otros recursos externos pueden utilizar sus propias direcciones.

## Revisar si un puerto está ocupado

### Windows PowerShell

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
netstat -ano | findstr :8082
netstat -ano | findstr :8083
netstat -ano | findstr :8084
netstat -ano | findstr :3000
```

El número de la última columna es el PID. Para identificar el proceso:

```powershell
tasklist /FI "PID eq NUMERO_PID"
```

### macOS o Linux

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:8082 -sTCP:LISTEN
lsof -nP -iTCP:8083 -sTCP:LISTEN
lsof -nP -iTCP:8084 -sTCP:LISTEN
lsof -nP -iTCP:3000 -sTCP:LISTEN
```

Si el comando no muestra resultados, no se detectó un proceso escuchando en ese puerto.

## Alcance actual

El frontend centraliza sus URLs y utiliza el Gateway como punto de entrada a los microservicios.

La observabilidad mediante métricas, trazas distribuidas y logs correlacionados queda para una fase posterior.
