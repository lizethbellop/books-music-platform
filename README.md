# Books & Music Platform

## Puertos locales del equipo

Cada servicio usa un puerto distinto, pero todas las integrantes deben conservar
la misma asignación en sus propias computadoras:

| Aplicación o servicio | Puerto | URL base |
| --- | ---: | --- |
| Frontend Flutter web | 3000 | `http://localhost:3000` |
| Music Service | 8080 | `http://localhost:8080/api/music` |
| Books Service | 8081 | `http://localhost:8081/books` |
| Profile Service | 8082 | `http://localhost:8082/api/profiles` |
| Users Service (reservado) | 8083 | `http://localhost:8083` |
| Social Service (reservado) | 8084 | `http://localhost:8084` |
| Communities Service (reservado) | 8085 | `http://localhost:8085` |
| Chat Service (reservado) | 8086 | `http://localhost:8086` |
| Recommendations Service (reservado) | 8087 | `http://localhost:8087` |

PostgreSQL puede permanecer en el puerto `5432` para todos los servicios. No
hay conflicto porque cada servicio usa una base de datos diferente.

## Antes de iniciar

1. Iniciar PostgreSQL.
2. Confirmar que existan las bases y usuarios requeridos por cada servicio.
3. Configurar las contraseñas y credenciales externas como variables de entorno.
4. Iniciar cada backend en una terminal distinta.
5. Iniciar Flutter al final y hacer un reinicio completo si se cambia una URL.

## Iniciar los servicios en Windows PowerShell

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

Usar los valores locales correspondientes a la base de datos y a Cloudinary:

```powershell
cd D:\books-music-platform\backend\profile-service
$env:PROFILE_DB_URL="jdbc:postgresql://localhost:5432/profile_db"
$env:PROFILE_DB_USERNAME="profile_user"
$env:PROFILE_DB_PASSWORD="TU_PASSWORD"
$env:CLOUDINARY_URL="TU_CLOUDINARY_URL"
.\mvnw.cmd spring-boot:run
```

### Frontend Flutter web — puerto 3000

Con los puertos predeterminados del repositorio basta con ejecutar:

```powershell
cd D:\books-music-platform\frontend
flutter run -d edge --web-port 3000
```

También se pueden indicar las tres APIs explícitamente:

```powershell
flutter run -d edge --web-port 3000 `
  --dart-define=MUSIC_API_URL=http://localhost:8080/api/music `
  --dart-define=BOOKS_API_URL=http://localhost:8081/books `
  --dart-define=PROFILE_API_URL=http://localhost:8082/api/profiles
```

## Iniciar los servicios en macOS o Linux

En terminales separadas:

```bash
cd backend/music-service
MUSIC_DB_PASSWORD='TU_PASSWORD' \
SPOTIFY_CLIENT_ID='TU_CLIENT_ID' \
SPOTIFY_CLIENT_SECRET='TU_CLIENT_SECRET' \
bash mvnw spring-boot:run
```

```bash
cd backend/books-service
BOOKS_DB_PASSWORD='TU_PASSWORD' bash mvnw spring-boot:run
```

```bash
cd backend/profile-service
PROFILE_DB_URL='jdbc:postgresql://localhost:5432/profile_db' \
PROFILE_DB_USERNAME='profile_user' \
PROFILE_DB_PASSWORD='TU_PASSWORD' \
CLOUDINARY_URL='TU_CLOUDINARY_URL' \
bash mvnw spring-boot:run
```

```bash
cd frontend
flutter run -d chrome --web-port 3000
```

## Revisar si un puerto está ocupado

### Windows

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
netstat -ano | findstr :8082
netstat -ano | findstr :3000
```

El número de la última columna es el PID. Para saber qué programa es:

```powershell
tasklist /FI "PID eq NUMERO_PID"
```

### macOS o Linux

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:8082 -sTCP:LISTEN
lsof -nP -iTCP:3000 -sTCP:LISTEN
```

Si el comando no muestra nada, el puerto está disponible.
