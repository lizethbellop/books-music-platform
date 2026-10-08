# Musa: preparar y ejecutar los cuatro servicios en Windows

Todos los bloques de comandos son para **PowerShell**. Trabajar con la revisión del repositorio que incluya esta guía y los últimos cambios; Liz todavía debe publicarlos para que aparezcan al descargar GitHub. Cada servicio mantiene su propia BD. No copiar los .env de otra integrante al repositorio.

## 1. Instalar y comprobar herramientas

Instalar Git, JDK **21**, PostgreSQL (16 o posterior) y Docker Desktop. Docker se utiliza aquí únicamente para Redis. PostgreSQL funciona instalado en Windows; los backend se ejecutan con Java. No hace falta instalar Maven: el repositorio incluye mvnw.cmd.

- [Git para Windows](https://git-scm.com/downloads/win)
- [JDK 21 — Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21)
- [PostgreSQL para Windows](https://www.postgresql.org/download/windows/)
- [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/)

Durante la instalación de PostgreSQL, conservar el puerto **5432** y recordar la contraseña del usuario administrador postgres. Docker Desktop debe estar encendido y configurado para contenedores Linux con WSL 2.

```powershell
git --version
java -version
docker version
```

Java debe indicar 21. Si instalaste Temurin en su ubicación habitual y tienes otra versión activa, en cada terminal que vaya a ejecutar un backend:

```powershell
$jdk = Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\jdk-21*" -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $jdk) { throw "No se encontró JDK 21; revisa la ubicación de instalación." }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Si usaste otro distribuidor, asignar a JAVA_HOME la carpeta de ese JDK 21.

## 2. Descargar el repositorio

Para la primera descarga:

```powershell
Set-Location "$env:USERPROFILE\Documents"
git clone https://github.com/lizethbellop/books-music-platform.git
Set-Location .\books-music-platform
$repo = (Get-Location).Path
```

Si ya tienes el repositorio, entrar a esa carpeta y ejecutar git status. Con el trabajo propio guardado, actualizar la rama acordada con el equipo usando git pull --ff-only. No reemplazar una carpeta que contenga cambios sin guardar.

```powershell
git status
git pull --ff-only
$repo = (Get-Location).Path
```

Los scripts ya vienen con el repositorio. No hace falta transcribirlos ni pegar SQL de capturas.

## 3. Preparar PostgreSQL y localizar sus comandos

El servicio de PostgreSQL debe estar iniciado; se puede comprobar con Servicios de Windows o con:

```powershell
Get-Service -Name '*postgres*'
Test-NetConnection localhost -Port 5432
$psql = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe" | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $psql) { throw "No se encontró psql.exe; revisa dónde instalaste PostgreSQL." }
$pgBin = Split-Path $psql.FullName
$env:Path = "$pgBin;$env:Path"
psql --version
```

Estas variables existen en esta terminal. Si abres otra terminal para SQL, repetir la localización. psql solicitará la contraseña del rol usado.

Si PostgreSQL aparece detenido, abrir PowerShell como administrador y ejecutar:

```powershell
$pgService = Get-Service -Name '*postgres*' | Select-Object -First 1
Start-Service -Name $pgService.Name
```

Volver a la terminal del repositorio para los pasos siguientes.

### Instalación nueva: crear cuatro roles y cuatro bases

Desde la raíz del repositorio:

```powershell
psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f .\backend\database\create_local_databases.sql
psql -h localhost -U postgres -d postgres
```

Dentro del prompt de **psql**, ejecutar lo siguiente. Cada comando pide una contraseña dos veces, sin mostrarla. Elegir contraseñas propias y guardarlas para los .env:

```text
\password auth_user
\password profile_user
\password books_user
\password music_user
\q
```

El primer script solo crea lo que no existe. Este recorrido de primera instalación supone bases nuevas: si ya existen, usar la sección de actualización y conservar sus propietarios/credenciales.

### Crear las tablas desde los scripts de Git

**Solo para bases nuevas**, ejecutar los scripts con su propietario. Ante cualquier error, detenerse y corregirlo antes de seguir:

```powershell
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\schema.sql
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\seed.sql
psql -h localhost -U profile_user -d profile_db -v ON_ERROR_STOP=1 -f .\backend\profile-service\database\schema.sql
psql -h localhost -U books_user -d books_db -v ON_ERROR_STOP=1 -f .\backend\books-service\database\schema.sql
psql -h localhost -U music_user -d music_db -v ON_ERROR_STOP=1 -f .\backend\music-service\database\schema.sql
```

Usuarios ya incluye username desde el inicio, con unicidad normal y sin distinguir mayúsculas. El seed de Usuarios agrega USUARIO y VENDEDOR, necesarios para registrar cuentas. No crea usuarios ni contraseñas de prueba. Libros y Música se cargan usando sus catálogos: para este recorrido no se necesita el seed de Música.

Comprobar:

```powershell
psql -h localhost -U auth_user -d BDAuth -c "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'users';"
psql -h localhost -U auth_user -d BDAuth -c "SELECT name FROM roles;"
psql -h localhost -U profile_user -d profile_db -c "\dt"
psql -h localhost -U books_user -d books_db -c "\dt"
psql -h localhost -U music_user -d music_db -c "\dt"
```

## 4. Si ya tienes bases de una versión anterior

Detener los backend antes de migrar. Conservar el usuario y contraseña que ya posee cada base; no ejecutar nuevamente los CREATE TABLE de Libros, Perfil o Música. Primero crear respaldos fuera del repositorio:

```powershell
$backup = Join-Path $env:USERPROFILE ("Documents\Musa-backup-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup
pg_dump -h localhost -U postgres -d BDAuth -Fc -f "$backup\BDAuth.dump"
pg_dump -h localhost -U postgres -d profile_db -Fc -f "$backup\profile_db.dump"
pg_dump -h localhost -U postgres -d books_db -Fc -f "$backup\books_db.dump"
pg_dump -h localhost -U postgres -d music_db -Fc -f "$backup\music_db.dump"
```

Si un respaldo falla, corregirlo antes de migrar. Los archivos pueden contener datos privados y hashes de contraseñas; no agregarlos a Git.

Autenticación, si aún no tiene username, ejecutar como postgres o el propietario de sus tablas:

```powershell
psql -h localhost -U postgres -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\migrations\001_username.sql
psql -h localhost -U postgres -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\seed.sql
```

La migración conserva las cuentas y asigna user_ más su UUID sin guiones a las que no tenían username. Es idempotente. No ejecutar schema.sql sobre una base antigua sin migrar primero.

Música, comprobar primero el tipo de user_id:

```powershell
psql -h localhost -U postgres -d music_db -c "SELECT table_name, data_type FROM information_schema.columns WHERE table_name IN ('music_rating','music_favorite','music_review') AND column_name = 'user_id';"
```

Si los tres ya son uuid, no hace falta convertirlos. Si son bigint, leer **docs/MIGRACION_UUID_MUSICA.md** antes de ejecutar:

```powershell
psql -h localhost -U postgres -d music_db -v ON_ERROR_STOP=1 -f .\backend\music-service\database\migrations\001_user_id_uuid.sql
```

La conversión de IDs numéricos antiguos usa el mapeo definido en ese script; no adivina a qué cuenta de autenticación pertenecían. Revisar dicho mapeo antes de migrar datos reales. Si el tipo es diferente de bigint o uuid, detenerse y revisar el esquema con la responsable de Música. Perfil y Libros ya usan UUID en sus esquemas actuales. Si Perfil no tiene las tablas de listas/preferencias o sus restricciones únicas, comparar su esquema con database/schema.sql; no borrar ni recrear una base con datos para forzar la actualización.

## 5. Redis para las sesiones

Con Docker Desktop iniciado, crear Redis una sola vez:

```powershell
docker run --name musa-redis -d -p 127.0.0.1:6379:6379 --restart unless-stopped -v musa-redis-data:/data redis:7-alpine redis-server --appendonly yes
docker exec musa-redis redis-cli ping
```

Debe responder **PONG**. Si el contenedor ya existe:

```powershell
docker start musa-redis
docker exec musa-redis redis-cli ping
```

No crear otra copia si ya tienes Redis escuchando en 6379. El volumen conserva las sesiones al reiniciar; los tokens mantienen su vencimiento.

## 6. Preparar los cuatro .env

Desde la raíz del repositorio, copiar cada plantilla solo si todavía no existe el .env:

```powershell
foreach ($service in 'users-service','profile-service','books-service','music-service') {
    $dir = Join-Path $repo "backend\$service"
    if (-not (Test-Path "$dir\.env")) {
        Copy-Item "$dir\.env.example" "$dir\.env"
    }
    notepad "$dir\.env"
}
```

Guardar con nombre exacto **.env**, no .env.txt. Escribir líneas CLAVE=valor, sin comillas alrededor del valor. Los secretos quedan solo en la computadora; .env está ignorado por Git. Los archivos .env.example deben conservar valores vacíos para contraseñas y API keys.

### Una misma clave JWT en los cuatro servicios de esa computadora

Generar una clave local Base64 de 64 bytes:

```powershell
$jwtBytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
[Convert]::ToBase64String($jwtBytes)
```

Copiar ese resultado como JWT_SECRET en **los cuatro .env de la misma instalación**. Cada integrante puede generar su propia clave para su entorno independiente. No publicar el resultado ni copiarlo a los contratos o tests.

### Usuarios — backend/users-service/.env

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/BDAuth
DB_USERNAME=auth_user
DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_AUTH_USER
JWT_SECRET=REEMPLAZAR_CON_LA_CLAVE_GENERADA
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
MAX_FAILED_ATTEMPTS=3
LOCK_TIME_MINUTES=5
MAIL_USERNAME=REEMPLAZAR_CON_TU_CORREO
MAIL_PASSWORD=REEMPLAZAR_CON_LA_CONTRASENA_DE_APLICACION_DEL_CORREO
USERS_SERVICE_PORT=8083
```

El correo de bienvenida forma parte del registro actual; configurar SMTP correctamente para probarlo. MAIL_PASSWORD es la contraseña de aplicación del proveedor, no la contraseña normal. Los parámetros actuales usan Gmail. No inventar valores ni dejar los marcadores REEMPLAZAR.

### Perfil — backend/profile-service/.env

```dotenv
PROFILE_DB_URL=jdbc:postgresql://localhost:5432/profile_db
PROFILE_DB_USERNAME=profile_user
PROFILE_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_PROFILE_USER
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
CLOUDINARY_URL=REEMPLAZAR_CON_TU_URL_DE_CLOUDINARY
PROFILE_SERVICE_PORT=8082
BOOKS_SERVICE_URL=http://localhost:8081
MUSIC_SERVICE_URL=http://localhost:8080
```

Cloudinary se necesita para subir fotos de perfil. Usar la credencial privada del dashboard local y no compartirla en Git.

### Libros — backend/books-service/.env

```dotenv
BOOKS_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_BOOKS_USER
DEEPL_API_KEY=REEMPLAZAR_CON_TU_CLAVE_DE_DEEPL
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
BOOKS_SERVICE_PORT=8081
```

La configuración actual usa books_db y books_user. Open Library no requiere API key; DeepL utiliza una clave propia del proveedor.

### Música — backend/music-service/.env

```dotenv
MUSIC_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_MUSIC_USER
SPOTIFY_CLIENT_ID=REEMPLAZAR_CON_TU_CLIENT_ID
SPOTIFY_CLIENT_SECRET=REEMPLAZAR_CON_TU_CLIENT_SECRET
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
MUSIC_SERVICE_PORT=8080
```

La configuración actual usa music_db y music_user. Spotify necesita las credenciales de una aplicación del proveedor. Los backend leen el .env como archivo de propiedades; no hace falta cargarlo con comandos Bash ni instalar dotenv.

Comprobar que Git los ignora:

```powershell
git check-ignore backend/users-service/.env backend/profile-service/.env backend/books-service/.env backend/music-service/.env
```

Debe listar los cuatro archivos.

## 7. Encender los cuatro backend

Abrir **cuatro terminales PowerShell**, una por servicio. En cada una configurar JDK 21 si es necesario. Sustituir la ruta si descargaste el repositorio en otro lugar. Dejar las terminales abiertas:

```powershell
# Terminal 1: Usuarios
Set-Location "$env:USERPROFILE\Documents\books-music-platform\backend\users-service"
.\mvnw.cmd spring-boot:run
```
```powershell
# Terminal 2: Perfil
Set-Location "$env:USERPROFILE\Documents\books-music-platform\backend\profile-service"
.\mvnw.cmd spring-boot:run
```
```powershell
# Terminal 3: Libros
Set-Location "$env:USERPROFILE\Documents\books-music-platform\backend\books-service"
.\mvnw.cmd spring-boot:run
```
```powershell
# Terminal 4: Música
Set-Location "$env:USERPROFILE\Documents\books-music-platform\backend\music-service"
.\mvnw.cmd spring-boot:run
```

Esperar **Started** en las cuatro. El primer arranque descarga dependencias y necesita internet. Puertos: Música 8080, Libros 8081, Perfil 8082, Usuarios 8083.

En otra terminal:

```powershell
8080,8081,8082,8083 | ForEach-Object { Test-NetConnection localhost -Port $_ }
Invoke-RestMethod http://localhost:8080/api/music/health
docker exec musa-redis redis-cli ping
```

Los puertos abiertos solo confirman que los procesos escuchan; el recorrido funcional siguiente verifica su comunicación.

## 8. Flutter y recorrido funcional

Con Flutter instalado y Chrome disponible:

```powershell
Set-Location "$env:USERPROFILE\Documents\books-music-platform\frontend"
flutter doctor
flutter pub get
flutter run -d chrome --web-port=3000
```

Para escritorio Windows, instalar las herramientas de C++ de Visual Studio que indica flutter doctor y seguir la [configuración oficial de Flutter para Windows](https://docs.flutter.dev/platform-integration/windows/setup); luego usar flutter run -d windows. Los comandos web permiten comprobar el proyecto sin preparar el compilador nativo.

1. Registrar una cuenta nueva con username propio e iniciar sesión por correo.
2. Abrir Perfil: aparece el username y se inicializa el perfil sin editar tablas.
3. Buscar preferencias de libro, canción y artista; revisar nombres, imágenes y eliminación.
4. Crear una lista y agregar esos tipos de contenido. Cerrar y abrir para comprobar persistencia; probar quitar un elemento y evitar duplicados.
5. Crear una reseña de libro y otra musical. Entrar con otra cuenta y comprobar el username de la autora.
6. Comprobar un único sidebar claro en Música y el avatar circular inferior.
7. Probar favoritos, calificaciones, edición/eliminación de reseñas y cierre de sesión.
8. Probar Recordarme al cerrar completamente el navegador y abrir la misma dirección localhost:3000. Sin marcarlo debe pedir login; con él debe conservar la sesión, hasta su límite absoluto de 7 días. El access token se renueva cerca de su vencimiento de 15 minutos.

Las direcciones localhost sirven para Chrome/Windows en la misma computadora. Un teléfono físico necesita direcciones accesibles en la red; esa configuración se prepara aparte.

## 9. Ejecutar las pruebas

Desde cada carpeta de servicio:

```powershell
.\mvnw.cmd '-DexcludedGroups=integration' test
```

Ese filtro ejecuta las pruebas sin la etiqueta integration; incluye unitarias y pruebas Spring. En Perfil las pruebas PostgreSQL están condicionadas por variables y se omiten normalmente. No interpretar omitidas como ejecutadas.

Perfil, para incluir sus pruebas locales de PostgreSQL:

```powershell
$env:MUSA_PROFILE_DB_TEST = 'true'
.\mvnw.cmd test
Remove-Item Env:MUSA_PROFILE_DB_TEST
```

Usuarios, prueba dirigida de registro/username con BDAuth y Redis reales, sin enviar correos:

```powershell
$env:MUSA_USERNAME_DB_TEST = 'true'
.\mvnw.cmd '-Dtest=UsernameFlowIntegrationTest' test
Remove-Item Env:MUSA_USERNAME_DB_TEST
```

Usuarios, sesiones reales de Redis:

```powershell
$env:MUSA_REDIS_TEST = 'true'
.\mvnw.cmd '-Dtest=RefreshSessionStoreRedisTest' test
Remove-Item Env:MUSA_REDIS_TEST
```

Las otras pruebas de repositorios de Usuarios usan por defecto una base **BDAuth_test** separada: no ejecutar toda la suite sin prepararla y revisar backend/users-service/src/test/resources/application.properties. No cambiar DB_URL para apuntar tests destructivos a una BD con datos personales.

Libros y Música, con sus bases listas y .env configurados, pueden ejecutar .\mvnw.cmd test para incluir integración. En frontend:

```powershell
flutter analyze
flutter test
```

El inventario actual está en CONTEO_PRUEBAS.md.

## 10. Problemas frecuentes y apagar

- **Port already in use**: una copia anterior sigue encendida. Detenerla con Ctrl+C; no abrir otra terminal para duplicarla.
- **password authentication failed**: el rol o contraseña de .env no coincide con PostgreSQL. No es la clave de Spotify ni de Gmail.
- **relation does not exist / schema validation**: faltó aplicar el schema correcto con el propietario correcto.
- **Connection refused en 6379**: encender Docker/Redis y comprobar PONG.
- **401 en catálogos**: comprobar JWT_SECRET idéntico en los cuatro backend y entrar otra vez.
- **Falla de registro por correo**: revisar MAIL_USERNAME/MAIL_PASSWORD y la terminal de Usuarios.
- **Diseño antiguo**: comprobar la revisión de Git, detener Flutter y reiniciar; en Chrome hacer recarga completa. Si persiste un build viejo, flutter clean y flutter pub get antes de ejecutar otra vez.
- **API externa falla**: revisar las credenciales locales y la conexión, sin publicar sus valores en capturas.

Apagar los backend y Flutter con Ctrl+C en sus terminales. Redis puede detenerse con docker stop musa-redis; no eliminar el volumen si quieres conservar las sesiones. Cada integrante conserva sus propios datos y secretos. Liz revisa los cambios y realiza el commit/push; esta guía no publica nada automáticamente.

Referencias oficiales: [PostgreSQL Windows](https://www.postgresql.org/download/windows/), [Docker Desktop Windows](https://docs.docker.com/desktop/setup/install/windows-install/), [Redis con Docker](https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/docker/).

Validación de entrega: los cuatro schemas se ejecutaron en bases temporales PostgreSQL 18; también se probaron la unicidad de username sin distinguir mayúsculas, los roles idempotentes y la migración sobre una cuenta anterior. Las consultas de creación se verificaron sin modificar roles/bases existentes. Los comandos PowerShell se revisaron, pero no se ejecutaron en Windows desde esta Mac.
