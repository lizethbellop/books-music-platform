# Windows: actualizar bases existentes y pruebas adicionales

Esta referencia usa PowerShell. Primero ubica psql como indica [la guía principal](GUIA_WINDOWS.md). No es el recorrido de instalación nueva ni de arranque diario.

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

