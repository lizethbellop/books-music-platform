# Musa en Mac: paso a paso

Usa la aplicación **Terminal**. [Explicación de programas, .env y pruebas manuales](GUIA_LOCAL.md). En este recorrido PostgreSQL y Redis se ejecutan directamente en la Mac: no necesitas Docker.

## Liz: para volver a probar con lo que ya tienes

Tu instalación ya tiene bases, username y los cuatro .env preparados. **No repitas la creación de bases ni cambies JWT_SECRET.**

1. Abre Postgres.app y comprueba que el servidor esté iniciado.
2. En Terminal ejecuta redis-cli ping. Si responde PONG, Redis ya está listo. Si no conecta, usa el paso 4.
3. Abre las cuatro terminales del paso 6 y espera Started.
4. Abre Flutter con el paso 7 y sigue el recorrido manual.

## 1. Herramientas (solo si faltan)

Necesitas Git, JDK 21, PostgreSQL, Redis y Flutter. No necesitas instalar Maven: el proyecto trae mvnw.

```bash
git --version
java -version
flutter --version
```

Para PostgreSQL puedes usar [Postgres.app](https://postgresapp.com/), como la instalación local de Liz. Para Java, [Temurin 21](https://adoptium.net/temurin/releases/?version=21). Para Flutter, sigue [instalación oficial](https://docs.flutter.dev/install).

Si tienes varias versiones de Java, selecciona 21 en **cada terminal backend**:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## 2. Entrar al repositorio

Liz, tu ruta actual es:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform"
```

Para otra computadora que aún no tiene el proyecto:

```bash
cd "$HOME/Documents"
git clone https://github.com/lizethbellop/books-music-platform.git
cd books-music-platform
```

Si ya lo tienes, guarda tu trabajo antes de actualizar:

```bash
git status
git pull --ff-only origin main
```

Los pasos de preparación siguientes van desde la raíz, donde están backend y frontend.

## 3. PostgreSQL: bases y tablas (solo primera instalación)

Abre Postgres.app e inicia el servidor en 5432. Con Postgres.app agrega los comandos a esta terminal:

```bash
export PATH="/Applications/Postgres.app/Contents/Versions/latest/bin:$PATH"
psql --version
pg_isready -h localhost -p 5432
```

Debe indicar accepting connections. El administrador en una instalación nueva de Postgres.app suele ser tu usuario de macOS. Comprueba:

```bash
psql -h localhost -d postgres -c 'SELECT current_user;'
```

Si tu instalación usa postgres, agrega -U postgres a los dos comandos de creación siguientes. No cambies usuarios existentes solo para coincidir con el ejemplo.

```bash
psql -h localhost -d postgres -v ON_ERROR_STOP=1 -f backend/database/create_local_databases.sql
psql -h localhost -d postgres
```

Ahora estás **dentro de psql**. Asigna contraseñas propias; las pide dos veces sin mostrarlas:

```text
\password auth_user
\password profile_user
\password books_user
\password music_user
\q
```

De vuelta en Terminal, carga los scripts que vienen en Git:

```bash
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f backend/users-service/database/schema.sql
psql -h localhost -U auth_user -d BDAuth -v ON_ERROR_STOP=1 -f backend/users-service/database/seed.sql
psql -h localhost -U profile_user -d profile_db -v ON_ERROR_STOP=1 -f backend/profile-service/database/schema.sql
psql -h localhost -U books_user -d books_db -v ON_ERROR_STOP=1 -f backend/books-service/database/schema.sql
psql -h localhost -U music_user -d music_db -v ON_ERROR_STOP=1 -f backend/music-service/database/schema.sql
```

Ante cualquier error, corrígelo antes de seguir. Usuarios ya incluye username y el seed carga los roles. No necesitas el seed de Música ni crear cuentas a mano.

### Si ya tienes bases con datos

Liz ya tiene aplicada la migración de username. Para otra instalación antigua: detener backend y respaldar primero, fuera del repositorio. Usar el administrador real de esas bases (este ejemplo usa postgres):

```bash
backup_dir="$HOME/Documents/Musa-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"
pg_dump -h localhost -U postgres -d BDAuth -Fc -f "$backup_dir/BDAuth.dump"
pg_dump -h localhost -U postgres -d profile_db -Fc -f "$backup_dir/profile_db.dump"
pg_dump -h localhost -U postgres -d books_db -Fc -f "$backup_dir/books_db.dump"
pg_dump -h localhost -U postgres -d music_db -Fc -f "$backup_dir/music_db.dump"
```

Si algún respaldo falla, corrígelo antes de migrar. Para Usuarios antiguo sin username:

```bash
psql -h localhost -U postgres -d BDAuth -v ON_ERROR_STOP=1 -f backend/users-service/database/migrations/001_username.sql
psql -h localhost -U postgres -d BDAuth -v ON_ERROR_STOP=1 -f backend/users-service/database/seed.sql
```

Para Música, comprobar antes de convertir:

```bash
psql -h localhost -U postgres -d music_db -c "SELECT table_name, data_type FROM information_schema.columns WHERE table_name IN ('music_rating','music_favorite','music_review') AND column_name = 'user_id';"
```

Si los tres tipos son uuid, no hagas nada. Si son bigint, sigue [migración de Música](MIGRACION_UUID_MUSICA.md): requiere un mapeo verificado antes de ejecutar el script. No adivines UUID ni borres la BD. Para tablas de Perfil anteriores, compara con su schema y revisa los cambios pendientes sin recrear una base con datos.

## 4. Encender Redis sin Docker

Primero:

```bash
redis-cli ping
```

Si responde PONG, ya está corriendo y puedes seguir. Si no está instalado y tienes Homebrew:

```bash
brew install redis
```

Para encenderlo y comprobarlo:

```bash
brew services start redis
redis-cli ping
```

Si tu Redis se instaló manualmente en lugar de Homebrew, abre una terminal dedicada:

```bash
redis-server --bind 127.0.0.1 --port 6379
```

Déjala abierta y comprueba redis-cli ping en otra terminal. Usa **un solo método**, no ambos a la vez. Referencia: [Redis para macOS](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/install-redis-on-mac-os/).

## 5. Crear .env (solo los que faltan)

Desde la raíz:

```bash
for service in users-service profile-service books-service music-service; do
  if [ ! -f "backend/$service/.env" ]; then
    cp "backend/$service/.env.example" "backend/$service/.env"
    chmod 600 "backend/$service/.env"
  fi
done
```

Abre esos archivos en tu editor. [Aquí están los campos y cómo llenarlos](GUIA_LOCAL.md#qué-escribir-en-los-env). Guarda tus contraseñas de BD y credenciales de Gmail, Cloudinary, DeepL y Spotify.

En una instalación nueva, genera **una sola clave**:

```bash
openssl rand -base64 64 | tr -d '\n'
printf '\n'
```

Copia el resultado completo en JWT_SECRET de los cuatro .env. Si ya tienes una clave funcional, úsala en los cuatro y no generes otra. En una instalación anterior también conserva los usuarios y contraseñas reales de BD; los ejemplos de la guía corresponden a bases nuevas.

## 6. Encender los cuatro backend

Abre cuatro terminales. Estos comandos usan la ruta de Liz: en otra Mac sustituye la carpeta del repositorio por la tuya. Deja las terminales abiertas y espera Started en cada una. Usamos bash mvnw para evitar el error permission denied.

Terminal 1 — Usuarios:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/users-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash mvnw spring-boot:run
```

Terminal 2 — Perfil:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/profile-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash mvnw spring-boot:run
```

Terminal 3 — Libros:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/books-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash mvnw spring-boot:run
```

Terminal 4 — Música:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/music-service"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash mvnw spring-boot:run
```

## 7. Abrir Flutter y probar

Quinta terminal, para escritorio Mac:

```bash
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/frontend"
flutter pub get
flutter run -d macos
```

Necesitas Xcode y la configuración que comprueba flutter doctor para escritorio. Si quieres navegador, desde la misma carpeta:

```bash
flutter run -d chrome --web-port=3000
```

Después sigue [el recorrido manual](GUIA_LOCAL.md#comprobar-el-flujo-como-lo-hicimos-en-local).

Para comprobar frontend:

```bash
flutter analyze
flutter test
```

Desde la carpeta de cada backend, las pruebas sin etiqueta integration:

```bash
bash mvnw -DexcludedGroups=integration test
```

Incluye pruebas unitarias y Spring; no significa que se ejecutaron las de BD/Redis real. Estas necesitan preparación adicional; consulta [inventario](CONTEO_PRUEBAS.md) y [configuración técnica](CONFIGURACION_LOCAL.md).

Para parar backend y Flutter, Ctrl+C en sus terminales. Redis de Homebrew se detiene con brew services stop redis; si lo ejecutaste en una terminal, Ctrl+C en ella. No hace falta borrar datos ni .env al terminar.
