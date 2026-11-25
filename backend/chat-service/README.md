# Chat privado de MUSA

La app tiene una sección **Mensajes**, independiente de Social. Permite buscar por
nombre de usuario, crear o recuperar un chat privado, listar conversaciones, enviar
texto y cargar mensajes anteriores. Los grupos no forman parte de este flujo.

## Ejecutar localmente

1. Enciende PostgreSQL y usa el esquema ya creado en `chatdb`.
   No hay migraciones nuevas ni es necesario volver a ejecutar SQL.
2. En `chat-service/.env` conserva las credenciales de BD y la misma `JWT_SECRET`
   Base64 de users-service. No compartas este archivo ni la clave con el frontend.
3. Inicia **users-service (8083)**, **chat-service (8088)** y **api-gateway (8084)**
   desde sus respectivas carpetas con `./mvnw spring-boot:run`.
4. En `frontend` ejecuta `flutter pub get` y arranca la app como habitualmente.
   `flutter run -d chrome --web-port 3000` fija un origen local predecible.
5. Inicia sesión, abre **Mensajes** en el menú, busca al menos dos caracteres del
   nombre de otra persona y selecciónala. Para ver ambos lados, usa dos sesiones
   de navegador independientes con usuarios distintos.

El frontend utiliza `GATEWAY_URL` (predeterminado `http://localhost:8084`). En un
emulador o dispositivo establece una dirección alcanzable desde ese dispositivo;
localhost se refiere al dispositivo donde corre el cliente.

## Configuración

- `CHAT_DB_URL`, `CHAT_DB_USERNAME`, `CHAT_DB_PASSWORD`: conexión del chat.
- `JWT_SECRET`: misma clave Base64 de users-service. HS512, al menos 64 bytes.
- `USERS_SERVICE_URL`: `http://localhost:8083` por defecto.
- `CHAT_SERVICE_PORT`: 8088 por defecto.
- `CHAT_ALLOWED_ORIGINS`: patrones de orígenes de WebSocket. Los valores por defecto
  permiten localhost/127.0.0.1 para desarrollo. En despliegue configura únicamente
  los dominios de la aplicación, incluyendo esquema y puerto cuando corresponda.
- Gateway: `CHAT_SERVICE_URL`, `CHAT_WS_URL` y `USERS_SERVICE_URL` permiten configurar
  los destinos; `MUSA_WEB_ORIGIN` configura el origen web permitido.

## API

Todos los endpoints REST de chat requieren `Authorization: Bearer <access token>`.
La identidad sale del JWT; no se acepta un senderId proporcionado por el cliente.

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/v1/chat/conversations/private` | Body: `{ "recipientId": "UUID" }`. Crear/recuperar; 200 en ambos casos. |
| GET | `/api/v1/chat/conversations?page=0&size=20` | Chats del usuario; items, hasMore y nextPage. |
| POST | `/api/v1/chat/conversations/{id}/messages` | Body: `{ "content": "Hola" }`; texto de 1 a 4000 caracteres. |
| GET | `/api/v1/chat/conversations/{id}/messages?size=30` | Historial del más reciente al más antiguo. |
| POST | `/api/v1/chat/realtime/tickets` | Ticket temporal de conexión WebSocket. |
| GET | `/api/v1/users/search?q=ana` | Búsqueda de usuarios activos por prefijo, entre 2 y 40 caracteres, hasta 20 resultados. |
| GET | `/api/v1/users/usernames?ids=UUID,UUID` | Nombres públicos de participantes activos. |

El historial devuelve `items`, `hasMore`, `nextCreatedAt` y `nextId`. Para la página
anterior envía **ambos** cursores como `beforeCreatedAt` y `beforeId`. Conserva la
fecha exacta recibida, sin redondearla; tamaño permitido de 1 a 50. Esta paginación
no desplaza el historial anterior cuando llegan mensajes nuevos.

Conversaciones ajenas o inexistentes producen el mismo 404. Los mensajes vacíos,
parámetros incorrectos y textos demasiado largos producen 400. Un token no válido
produce 401. Los fallos temporales de usuarios/BD o espera del bloqueo producen
503 con mensajes controlados; no se muestran consultas SQL ni trazas al cliente.
Un error de configuración/BD al arrancar puede impedir el inicio del servicio:
el manejador HTTP solo está disponible después del arranque.

## Tiempo real

El socket es `ws://localhost:8084/api/v1/chat/ws` a través del gateway. Con HTTPS
se utiliza `wss://` automáticamente desde la app.

1. Obtener ticket por REST autenticado.
2. Abrir el socket y enviar como primer frame `{ "ticket": "..." }` antes de 5 segundos.
3. El servidor responde `{ "type": "ready" }`.
4. Los mensajes se envían por REST. Solo DESPUÉS del commit se publica:
   `{ "type": "message.created", "message": { ... } }` a los participantes.
5. El cliente envía `{ "type": "ping" }` y recibe `{ "type": "pong" }`.

El ticket es aleatorio, dura hasta 30 segundos y se consume una sola vez. No viaja
en la URL ni contiene el JWT. No hay suscripciones a IDs arbitrarios de usuarios.
Las conexiones se cierran cuando vence el access token; la app obtiene un ticket
nuevo con su sesión renovada y reconecta. Al cerrar/cambiar de sesión se cierra el
socket y se oculta el historial anterior.

Los mensajes se almacenan aunque el destinatario esté desconectado. Al reconectar,
la app vuelve a consultar el historial persistido; los eventos en vivo no son una
cola durable. El cliente combina respuestas/eventos por ID para evitar mostrar el
mismo mensaje dos veces. Si una petición de envío agota el tiempo después del commit,
consulta el historial antes de reenviar: no se reintenta el envío automáticamente.

## Pruebas

Sin tus servicios ni BD:

```sh
./mvnw -Dtest=PrivateEndpointBlockTest,EndpointSecurityBlockTest test
```

Prueba de integración real, exclusivamente con un cluster PostgreSQL temporal:

```sh
python3 scripts/run-isolated-tests.py
```

El script requiere Java 21, Maven y los binarios initdb, pg_ctl, createdb y psql.
En macOS busca primero Postgres.app y después PATH. Crea su propio directorio,
puerto y contraseña aleatorios; no usa .env ni tu servidor. Al terminar detiene
el cluster y borra sus datos. Prueba concurrencia, permisos, persistencia,
paginación y WebSocket real. `ChatFlowIntegrationTest` se omite en la ejecución
normal si no se activa el entorno aislado que configura este script.

Pruebas de búsqueda y gateway, desde cada servicio:

```sh
./mvnw -Dtest=ChatUserDirectoryTest test
./mvnw -Dtest=ChatGatewayRoutesTest test
```

Desde frontend: `flutter analyze` y `flutter test`.

## Alcance de despliegue

Esta implementación usa una instancia de chat-service: tickets y conexiones están
en memoria. Para varias instancias hace falta compartir tickets y distribuir los
eventos con un broker; los mensajes ya son persistentes. El bloqueo PostgreSQL
protege las creaciones que usan el flujo del servicio, no escrituras SQL externas.

HTTPS/WSS y certificados deben configurarse antes del despliegue público. OpenSSL
puede ayudar a inspeccionar certificados/TLS o preparar certificados locales, pero
no es necesario para el JWT simétrico actual. No se generaron ni rotaron claves,
certificados ni credenciales como parte de este cambio.

Referencias: [Spring WebSocket](https://docs.spring.io/spring/reference/7.0/web/websocket/server.html)
y [cliente Flutter](https://pub.dev/packages/web_socket_channel).


## Chat grupal

La sección Mensajes permite crear grupos separados de Social. El creador administra
el grupo. Se requieren nombre de 1 a 100 caracteres y de 1 a 49 personas adicionales;
el total máximo es 50, contando cuentas temporalmente inactivas que sigan siendo miembros.
La administración puede cambiar el nombre, agregar/retirar personas y transferirse.
No hay incorporación pública: el administrador agrega participantes registrados activos.

Cada participante ve mensajes desde su última incorporación. Al salir o ser retirado
pierde acceso a detalles, envío e historial. Los mensajes y su autoría permanecen
para quienes conservan acceso. La reincorporación actualiza joined_at y comienza
un historial nuevo para esa persona, sin duplicar ni borrar la fila del participante.

Al salir el administrador se asigna el relevo activo con incorporación más antigua
(desempate por UUID). Si no queda ninguna cuenta disponible, el grupo se cierra y
sus membresías se inactivan. Los grupos cerrados se conservan en BD, sin acceso ni
reincorporaciones desde la API actual. Un grupo puede quedar con una sola persona;
al salir la última se cierra.

Envíos y cambios de miembros bloquean la misma fila de conversación para evitar
superar límites o enviar desde una membresía ya retirada. Se filtran cuentas
inactivas en la entrega de mensajes grupales sin bloquear al resto del grupo.
WebSocket emite conversation.updated después del commit para actualizar metadatos
(y avisar al cliente retirado que deje de mostrar el contenido). Las operaciones
REST siempre vuelven a comprobar permisos, incluso si el cliente está desconectado.
No es posible retirar datos que una persona ya hubiera visto o guardado antes.

| Método | Ruta bajo /api/v1/chat/conversations | Uso |
| --- | --- | --- |
| POST | /groups | Body: name, participantIds. El creador se agrega automáticamente. |
| GET | /{id} | Detalles privados/grupales solo para miembros actuales. |
| PATCH | /{id}/group | Body: name. Solo administración. |
| POST | /{id}/participants | Body: userId. Agregar o reincorporar. |
| DELETE | /{id}/participants/{userId} | Retirar sin borrar mensajes. |
| POST | /{id}/admin | Body: userId. Transferir a un miembro activo. |
| POST | /{id}/leave | Salir; 204. Solo grupos. |

El listado y los endpoints de mensajes existentes ahora incluyen grupos. Las
respuestas añaden name y ownerId; en privados son null. No es necesario cambiar
el formato de los mensajes ni sus cursores.

### Actualizar una instalación existente

1. Aplicar notification-service/database/migrations/002_group_notifications.sql en
   notifications_db como su propietario.
2. Aplicar chat-service/database/migrations/002_groups.sql en chatdb como su propietario.
3. Reiniciar primero notification-service, después chat-service y actualizar Flutter.
   El gateway no requiere rutas nuevas; ya enruta /api/v1/chat/**.

Las migraciones son transaccionales y repetibles. No eliminan mensajes ni
notificaciones. Si existen grupos creados manualmente sin nombre/administración,
se les asigna un nombre genérico y el primer participante activo; los vacíos se
cierran. El esquema inicial actualizado incluye las nuevas columnas/restricciones.
Las credenciales y la outbox existente no cambian.

Los mensajes de grupo generan CHAT_MESSAGE_CREATED para cada participante activo
excepto el remitente. Altas, retiros administrativos y transferencias generan
GROUP_MEMBER_ADDED, GROUP_MEMBER_REMOVED y GROUP_ADMIN_CHANGED. No contienen texto
privado ni el nombre del grupo. Las actualizaciones de nombre y salidas también
actualizan clientes por WebSocket, sin crear notificaciones redundantes.

La batería completa con PostgreSQL y RabbitMQ aislados se ejecuta desde
notification-service con python3 scripts/run-isolated-tests.py. Incluye concurrencia,
reincorporación, preservación de autores, permisos, cierre, entrega y notificaciones.
