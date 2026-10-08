# Musa en Windows: paso a paso

Usa **PowerShell** para estos comandos. Completa un paso y comprueba su resultado antes de seguir. Todo se ejecuta en tu computadora. [Qué hace cada programa, .env y recorrido de prueba](GUIA_LOCAL.md).

## Si ya lo preparaste antes: solo encender

1. Enciende PostgreSQL desde Servicios de Windows.
2. En PowerShell ejecuta `docker start musa-redis` y después `docker exec musa-redis redis-cli ping`. Debe decir PONG. La primera instalación está en el paso 4.
3. Abre las cuatro terminales de backend del paso 6.
4. Abre Flutter con el paso 7.

No recrees bases, no copies plantillas encima de tus .env y no generes otro JWT cada día. Si acabas de actualizar una versión antigua, revisa [migraciones](GUIA_WINDOWS_ACTUALIZACION.md).

## 1. Tener las herramientas

Instala Git, JDK **21**, PostgreSQL y Flutter. No necesitas instalar Maven: usamos mvnw.cmd del repositorio. Para Redis usaremos el comando de Docker del paso 4. Necesitas tener Docker instalado y funcionando con contenedores Linux; esta guía no exige una aplicación específica. Java, PostgreSQL y Flutter siguen ejecutándose en Windows.

- [Git](https://git-scm.com/downloads/win)
- [JDK 21](https://adoptium.net/temurin/releases/?version=21)
- [PostgreSQL](https://www.postgresql.org/download/windows/)
- [Flutter Windows](https://docs.flutter.dev/install)
- [Redis con Docker](https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/docker/)

Al instalar PostgreSQL conserva el puerto 5432 y guarda la contraseña de postgres. Comprueba Docker siguiendo el paso 4.

```powershell
git --version
java -version
flutter --version
```

Java debe decir 21. Si no, selecciona tu JDK 21 en JAVA_HOME y agrega su carpeta bin a Path. Con Temurin en su ubicación habitual, en cada terminal backend:

```powershell
$jdk = Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\jdk-21*" -Directory | Select-Object -First 1
if (-not $jdk) { throw "Instala JDK 21 o revisa su ruta." }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## 2. Entrar al proyecto

Si todavía no lo tienes:

```powershell
Set-Location "$env:USERPROFILE\Documents"
git clone https://github.com/lizethbellop/books-music-platform.git
Set-Location .\books-music-platform
```

Si ya lo tienes, abre su carpeta en PowerShell, guarda tu trabajo y actualiza:

```powershell
git status
git pull --ff-only origin main
```

Los siguientes comandos de preparación se ejecutan desde esa raíz, donde están backend y frontend.

## 3. Crear bases y tablas (solo primera instalación)

Enciende PostgreSQL desde Servicios de Windows. Localiza sus comandos en esta terminal:

```powershell
$psql = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe" | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $psql) { throw "Revisa dónde instalaste PostgreSQL." }
$env:Path = "$(Split-Path $psql.FullName);$env:Path"
psql --version
Test-NetConnection localhost -Port 5432
```

Debe aparecer TcpTestSucceeded: True. Ahora crea las cuatro bases y sus usuarios:

```powershell
psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f .\backend\database\create_local_databases.sql
psql -h localhost -U postgres -d postgres
```

El segundo comando abre psql: verás postgres=#. **Dentro de psql**, escribe una línea a la vez; cada contraseña se pide dos veces y no se muestra:

```text
\password auth_user
\password profile_user
\password books_user
\password music_user
\q
```

Guarda las cuatro contraseñas: van en los .env. Ya de vuelta en PowerShell, copia estos comandos para cargar los scripts que vienen en Git:

```powershell
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\schema.sql
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f .\backend\users-service\database\seed.sql
psql -h localhost -U profile_user -d profile_db -v ON_ERROR_STOP=1 -f .\backend\profile-service\database\schema.sql
psql -h localhost -U books_user -d books_db -v ON_ERROR_STOP=1 -f .\backend\books-service\database\schema.sql
psql -h localhost -U music_user -d music_db -v ON_ERROR_STOP=1 -f .\backend\music-service\database\schema.sql
```

Cada comando pide la contraseña de su usuario. Ante un error, corrígelo antes de continuar. Usuarios ya trae username; el seed agrega los roles para registrarse. No necesitas seed de Música ni crear cuentas a mano.

**Si ya hay datos:** no hagas este paso como instalación nueva. Sigue [actualización con respaldos](GUIA_WINDOWS_ACTUALIZACION.md).

## 4. Encender Redis

**Si ya tienes Redis funcionando en localhost:6379, conserva esa instalación y salta la creación del contenedor.**

Si vas a usar Docker, primero comprueba desde PowerShell:

```powershell
docker version
```

Debe mostrar **Client y Server**, sin error de conexión. Si dice que docker no existe, falta instalar Docker. Si muestra Client pero no puede conectar con Server, falta encender el motor. `docker run` no instala ni enciende Docker por sí solo. Necesitas un motor compatible con contenedores Linux; no se exige Docker Desktop como requisito específico.

### Primera vez: crear Redis

```powershell
docker run --name musa-redis -d -p 127.0.0.1:6379:6379 --restart unless-stopped -v musa-redis-data:/data redis:7-alpine redis-server --appendonly yes
docker exec musa-redis redis-cli ping
```

Debe responder **PONG**. El primer comando descarga Redis y crea el contenedor. Solo se ejecuta una vez. El volumen musa-redis-data conserva sus datos.

### Las siguientes veces: encender el Redis que ya creaste

```powershell
docker start musa-redis
docker exec musa-redis redis-cli ping
Test-NetConnection localhost -Port 6379
```

Debe responder PONG y TcpTestSucceeded: True. Ya puedes seguir con los backend; se conectan a localhost:6379.

Si aparece que el nombre musa-redis ya está ocupado, usa docker start; no repitas docker run. Si el puerto 6379 está ocupado por un Redis que ya funciona, no arranques otra copia.

## 5. Crear y llenar los .env

Desde la raíz, crea solo los que faltan:

```powershell
foreach ($service in 'users-service','profile-service','books-service','music-service') {
    $folder = ".\backend\$service"
    if (-not (Test-Path "$folder\.env")) { Copy-Item "$folder\.env.example" "$folder\.env" }
}
```

Genera **una sola clave JWT**, solo si es una instalación nueva:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Copia el resultado en JWT_SECRET de los cuatro archivos. Después abre uno a la vez:

```powershell
notepad .\backend\users-service\.env
notepad .\backend\profile-service\.env
notepad .\backend\books-service\.env
notepad .\backend\music-service\.env
```

[En esta sección están los valores que debes llenar](GUIA_LOCAL.md#qué-escribir-en-los-env). Completa contraseñas de BD y tus credenciales de Gmail, Cloudinary, DeepL y Spotify. No dejes REEMPLAZAR ni claves vacías. Si ya tenías .env funcionales, consérvalos.

## 6. Encender los cuatro backend

Abre cuatro PowerShell, una por servicio. Estas rutas suponen que lo descargaste en Documents; ajusta si está en otra carpeta. Deja las cuatro abiertas y espera Started en cada una.

Terminal 1 — Usuarios:

```powershell
cd "$env:USERPROFILE\Documents\books-music-platform\backend\users-service"
.\mvnw.cmd spring-boot:run
```

Terminal 2 — Perfil:

```powershell
cd "$env:USERPROFILE\Documents\books-music-platform\backend\profile-service"
.\mvnw.cmd spring-boot:run
```

Terminal 3 — Libros:

```powershell
cd "$env:USERPROFILE\Documents\books-music-platform\backend\books-service"
.\mvnw.cmd spring-boot:run
```

Terminal 4 — Música:

```powershell
cd "$env:USERPROFILE\Documents\books-music-platform\backend\music-service"
.\mvnw.cmd spring-boot:run
```

## 7. Abrir Flutter y probar

Quinta terminal:

```powershell
cd "$env:USERPROFILE\Documents\books-music-platform\frontend"
flutter pub get
flutter run -d chrome --web-port=3000
```

Para escritorio Windows, primero prepara Visual Studio con C++ según [Flutter Windows](https://docs.flutter.dev/platform-integration/windows/setup), comprueba flutter doctor y usa flutter run -d windows.

Ahora sigue [el recorrido manual](GUIA_LOCAL.md#comprobar-el-flujo-como-lo-hicimos-en-local). Para parar, Ctrl+C en cada backend y Flutter. Para detener Redis, ejecuta `docker stop musa-redis`. No borres su volumen al terminar.

Esta guía se revisó desde Mac; los comandos PowerShell no se ejecutaron en una computadora Windows. Los schemas sí se probaron en PostgreSQL temporal.
