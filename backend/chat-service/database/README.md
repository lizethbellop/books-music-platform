# Base de datos del chat de MUSA

Esquema inicial: `database/schema.sql`. Sigue la carpeta y nombres snake_case de
`users-service/database/schema.sql`, con UUID y `gen_random_uuid()`. Requiere
PostgreSQL 13 o superior; no necesita extensiones. Cada compañera crea su BD local.
No contiene contraseñas ni datos de ejemplo.

## 1. Entrar desde Terminal en macOS

Abre Terminal y comprueba el cliente:

```sh
psql --version
```

Si no aparece, localiza `psql` en tu instalación. Con Postgres.app suele estar en
`/Applications/Postgres.app/Contents/Versions/latest/bin/psql`; puedes usar esa ruta
en lugar de `psql`. Con Homebrew, revisa tu fórmula instalada con `brew list --versions`
y agrega su carpeta `bin` al PATH (por ejemplo, `brew --prefix postgresql@16`
si esa es tu versión). No instales ni arranques una segunda instancia.

Comprueba que tu servidor esté iniciado desde Postgres.app o tu instalación habitual.
Si usas Homebrew, `brew services list` muestra su estado; inicia únicamente la
fórmula que ya utilizas, por ejemplo `brew services start postgresql@16`.

Conecta con el rol que ya tienes en PostgreSQL:

```sh
psql -X -h localhost -p 5432 -U lizbello -d postgres
```

Sustituye `lizbello` por tu rol administrador real y ajusta el puerto si es distinto.
En algunas instalaciones es `postgres`; en otras coincide con tu usuario de Mac
(`whoami`). La contraseña solicitada es la del rol de PostgreSQL.
Si dice `role ... does not exist`, revisa el nombre; si dice `connection refused`,
revisa servidor y puerto. No cambies la autenticación para omitir la contraseña.

## 2. Crear chat_user y chatdb manualmente

Entra primero como administradora (tu rol `lizbello`, si tiene CREATEROLE y CREATEDB):

```sh
psql -X -h localhost -p 5432 -U lizbello -d postgres
```

Si ves `postgres-#`, hay texto pendiente. `\r` lo limpia; espera `postgres=#`.
Ejecuta una línea a la vez:

```sql
\r
SELECT current_user;
\du
CREATE ROLE chat_user LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
\password chat_user
CREATE DATABASE chatdb OWNER chat_user;
REVOKE ALL ON DATABASE chatdb FROM PUBLIC;
GRANT CONNECT ON DATABASE chatdb TO chat_user;
\connect chatdb
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO chat_user;
\q
```

`\password` pide la nueva contraseña dos veces sin mostrarla. Guárdala localmente
para usarla en `.env`; no la compartas en el chat. Si el rol o la BD ya existen,
no los borres: verifica propietario y permisos antes de seguir. Si falta permiso,
usa el rol administrador de tu instalación. No ejecutes CREATE DATABASE dentro de BEGIN.
Estos comandos de preparación son manuales, no forman parte de schema.sql.

Con esto se retiran los permisos de PUBLIC para esta BD y se usa chat_user como
propietario para desarrollo local. Los superusuarios siguen teniendo acceso;
no significa que se bloquee a los administradores ni restringe chat_user en otras
bases que concedan CONNECT a PUBLIC. Esas otras bases no se modifican.
La autenticación por contraseña también depende de pg_hba.conf: si usa trust,
la contraseña no se comprueba. No cambies reglas globales sin revisar tu instalación.

## 3. Crear las tablas manualmente con el archivo

Sal con `\q` y, desde Terminal, entra a `backend/chat-service` de tu copia del repo.
Ejemplo para Liz (cada compañera debe cambiar la ruta):

```sh
cd "/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/chat-service"
psql -X -h localhost -p 5432 -U chat_user -d chatdb -v ON_ERROR_STOP=1 -f database/schema.sql
```

Alternativa si sigues dentro de `psql`, conectada a chatdb:

```sql
\set ON_ERROR_STOP on
\i '/ruta/absoluta/books-music-platform/backend/chat-service/database/schema.sql'
```

Elige una alternativa. El archivo contiene `BEGIN`/`COMMIT`: un error impide
confirmar cambios parciales. En una sesión interactiva, usa `ROLLBACK;` después
de un error antes de intentarlo nuevamente. `ON_ERROR_STOP` detiene la lectura.

Se puede repetir sobre el mismo esquema sin borrar datos gracias a `IF NOT EXISTS`.
Esto NO verifica ni corrige tablas o índices preexistentes con definiciones distintas.
Para una BD antigua con otras estructuras hay que preparar una migración específica.

## 4. Comprobar el resultado

Conéctate otra vez a chatdb y ejecuta:

```sql
\conninfo
\dt public.*
\d public.conversations
\d public.conversation_participants
\d public.messages
\di public.*
SELECT 'conversations' AS tabla, count(*) FROM public.conversations
UNION ALL SELECT 'conversation_participants', count(*) FROM public.conversation_participants
UNION ALL SELECT 'messages', count(*) FROM public.messages;
\q
```

En una instalación nueva deben verse las tres tablas vacías, sus restricciones
y los índices adicionales (más los índices creados por las claves primarias).

## Diseño y límites

- `conversations`: UUID, tipo `PRIVATE` o `GROUP` y fecha de creación.
- `conversation_participants`: PK compuesta para impedir participantes duplicados.
- `messages`: texto obligatorio con al menos un carácter que no sea espacio,
  tabulación o salto de línea; FK compuesta para exigir que el remitente sea participante.
- Borrar una conversación elimina sus mensajes y participantes. No se puede quitar
  un participante que tenga mensajes: conserva su registro para preservar el historial.
  Una futura salida de grupo debe modelarse con estado/fecha de salida y una migración.
- Los UUID de usuario son referencias lógicas a `users-service` (BDAuth); no hay FK
  entre bases. El backend debe verificar identidad, existencia del usuario y permisos.
- La aplicación deberá crear conversación y participantes en una transacción,
  exigir exactamente dos usuarios distintos en chats privados e impedir chats privados
  duplicados entre el mismo par. El esquema inicial no impone esas reglas entre filas.
- Las fechas usan `TIMESTAMP WITH TIME ZONE` para representar instantes; PostgreSQL
  las muestra en la zona de la sesión. Al crear entidades Java, mapearlas a `Instant`
  u `OffsetDateTime` y el tipo a enum almacenado como texto.

## Conectar Spring Boot con .env

La configuración importa `.env` desde el directorio de trabajo como Java Properties.
Desde chat-service:

```sh
cp -n .env.example .env
```

Edita `.env` localmente y completa `CHAT_DB_PASSWORD` con la contraseña asignada
con `\password chat_user`. No uses comillas ni `export`: este archivo se interpreta
como Properties, no como script de shell. Para una contraseña con barra invertida,
escribe cada barra como `\\`. No ejecutes `source .env`.

```properties
CHAT_DB_URL=jdbc:postgresql://localhost:5432/chatdb
CHAT_DB_USERNAME=chat_user
CHAT_DB_PASSWORD=TU_CONTRASEÑA_LOCAL
```

`.env` está ignorado por Git; `.env.example` contiene solo valores de ejemplo.
Variables de entorno reales con los mismos nombres tienen prioridad. Ejecuta desde
chat-service (en IntelliJ, fija ese directorio como Working directory):

```sh
./mvnw spring-boot:run
```

Primero crea la BD y aplica schema.sql como chat_user. La conexión ya no está excluida;
si falta la BD o la contraseña, el servicio puede fallar al iniciar. Se usa
`ddl-auto=validate` y `spring.sql.init.mode=never`: Spring no crea las tablas ni ejecuta
el script automáticamente. Aún faltan las entidades y repositorios del chat;
validate comprobará sus mapeos cuando existan. Las futuras modificaciones requieren
migraciones explícitas. No se ejecutó SQL ni se inició el servicio al preparar estos archivos.

Referencias: [PostgreSQL psql](https://www.postgresql.org/docs/current/app-psql.html),
[roles](https://www.postgresql.org/docs/current/sql-createrole.html) y
[configuración de Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).
