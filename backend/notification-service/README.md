# Notificaciones de MUSA y AMQP

Este servicio consume eventos de RabbitMQ, guarda una notificación por destinatario
y expone una API autenticada. La app tiene una campana independiente de Mensajes y
Social. No se incluyen correo ni push del sistema operativo.

## Recorrido

```text
Acción + outbox (misma transacción PostgreSQL)
    -> publicador pendiente -> RabbitMQ musa.events
    -> binding por routing key -> cola musa.notifications
    -> listener -> NotificationService -> notifications_db -> ACK
    -> API REST -> campana y pantalla del frontend
```

El exchange distribuye; el binding es la regla; la cola conserva eventos pendientes.
Un publisher confirm significa que RabbitMQ aceptó la publicación, no que el
consumidor terminó. El ACK del consumidor llega después de confirmar el guardado.
Marcar como leída en la app es otra operación: actualiza read_at en PostgreSQL.

## Configuración local

RabbitMQ vive en Docker; los servicios Spring pueden ejecutarse desde IntelliJ.
En backend/.env.rabbitmq define RABBITMQ_USER y RABBITMQ_PASSWORD. Este archivo
local está ignorado por Git. Levanta el broker desde backend:

```sh
docker compose --env-file .env.rabbitmq -f compose.rabbitmq.yml up -d
bash apply-rabbitmq-policies.sh
```

La política configura el destino de los rechazos de la cola existente sin borrarla.
Puedes ejecutarla antes de iniciar notification-service; Spring declarará después
el exchange/cola de fallidos y los bindings.

En notification-service/.env conserva NOTIFICATION_DB_URL, NOTIFICATION_DB_USERNAME,
NOTIFICATION_DB_PASSWORD y agrega la misma JWT_SECRET de users-service/chat-service.
No generes otra clave solo para este servicio. Las credenciales del broker se leen
además desde ../.env.rabbitmq. Inicia desde la carpeta del servicio:

```sh
./mvnw spring-boot:run
```

Puerto 8090; gateway 8084. Reinicia el gateway cuando cambien sus rutas.
En frontend ya están integrados la campana, lista, filtro de no leídas, contador,
marcado individual y marcar todas. La lista se refresca cada 15 segundos mientras
está abierta y tiene una sola página cargada; las listas extensas se actualizan
manualmente para conservar la posición. La campana consulta al entrar, al regresar
a la app y al cerrar la pantalla de notificaciones. No es push móvil.

## Activar los productores

Chat y Social registran eventos pendientes sin llamar a RabbitMQ desde la petición.
Cada base del productor necesita su propia tabla notification_outbox:

```sh
# Desde chat-service, conectado con el propietario de chatdb:
psql -h localhost -U chat_user -d chatdb -v ON_ERROR_STOP=1 -f database/migrations/001_notification_outbox.sql
# Desde social-service, con el propietario de social_db:
psql -h localhost -U social_user -d social_db -v ON_ERROR_STOP=1 -f database/migrations/001_notification_outbox.sql
```

Después define NOTIFICATION_EVENTS_ENABLED=true en el .env de cada productor y
reinícialo. Sin esa variable la integración nueva está desactivada para permitir
instalar primero la tabla. No basta con arrancar notification-service: necesitas
productores activados para recibir eventos reales.

La outbox se guarda dentro de la transacción de mensaje/seguimiento. Si falla el
registro del evento, esa acción también se revierte; si RabbitMQ está caído, la
acción sí puede confirmarse y el evento queda pendiente. El publicador usa mensajes
persistentes, mandatory, confirms y returns: solo marca publicado después de
confirmación positiva y sin devolución por falta de ruta. Si el proceso cae entre
confirmación y actualización, puede repetir; el consumidor evita duplicados con
UNIQUE(event_id, recipient_id), sin reiniciar read_at de notificaciones existentes.

Los pendientes se reintentan con espera progresiva de hasta 300 segundos. No se
borra la outbox automáticamente; definir retención de filas publicadas al desplegar.

## Contrato de eventos v1

La definición compartida está en contracts/events/notification-event.v1.schema.json.
Se publica JSON UTF-8, sin clases Java en cabeceras ni texto privado del mensaje:

```json
{
  "version": 1,
  "eventId": "39f4060e-b948-4892-8aac-eaf52b0f08a4",
  "type": "CHAT_MESSAGE_CREATED",
  "recipientId": "0260ef84-a502-4b83-befd-7cd588488a80",
  "actorId": "0602213f-c1cf-42e4-881a-dca826684261",
  "targetId": "48829935-c41f-4dd8-a39b-58459b5cfabd"
}
```

Los UUID del ejemplo son ficticios. No son datos para publicar en la BD real.

| Tipo | Routing key | Destinatario | targetId |
| --- | --- | --- | --- |
| CHAT_MESSAGE_CREATED | chat.message.created | Otro participante | Conversación |
| USER_FOLLOWED | social.user.followed | Persona seguida | Perfil del actor |
| POST_LIKED | social.post.liked | Autor de la publicación | Publicación |
| POST_COMMENTED | social.post.commented | Autor de la publicación | Publicación |
| POST_PUBLISHED | social.post.published | Cada seguidor seleccionado por el productor | Publicación |

Chat y seguimientos ya producen sus eventos. Para likes, comentarios y publicaciones
están listos contrato, validación y generación de notificaciones; falta que las
operaciones sociales futuras publiquen sus eventos. El productor debe resolver los
destinatarios y los permisos. El consumidor no consulta seguidores ni transmite a
todos los usuarios. Mantén eventId estable al reintentar la misma entrega.

## Fallos y DLQ

El listener admite hasta 16 KiB, convierte el JSON a un DTO y valida que tipo y
routing key coincidan. El servicio valida campos/versión y omite acciones sobre el
mismo usuario. Las notificaciones se insertan atómicamente sin duplicar.

Hay un intento inicial y dos reintentos, con esperas de uno y dos segundos. Al
agotarse, RejectAndDontRequeueRecoverer rechaza sin reencolar. La política lleva el
evento a musa.notifications.dlx y de ahí a musa.notifications.dlq. Los eventos
inválidos también pasan por este límite; no se repiten indefinidamente.

La DLQ no tiene consumidor automático. Corrige la causa, identifica la routing key
original y vuelve a publicar conservando eventId; comprueba después la persistencia.
No purgues ni reenvíes toda la DLQ sin revisar su contenido. Si la BD se cae por más
tiempo que los reintentos, el evento requiere recuperación desde esta cola.

Esto configura la recuperación local; para un despliegue crítico revisar quorum
queues, disponibilidad y garantías de dead lettering, TLS/AMQPS y permisos separados
por servicio. La cola durable y los confirms no equivalen a entrega exactamente una vez.

## API

Usar Authorization: Bearer con el access token de MUSA. El destinatario se toma
únicamente del JWT validado (HS512, issuer musa-auth, audience musa-api, subject UUID).

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | /api/v1/notifications?page=0&size=20&unreadOnly=false | items, hasMore, nextPage |
| GET | /api/v1/notifications/unread-count | count |
| POST | /api/v1/notifications/{id}/read | 204; conserva la primera fecha de lectura |
| POST | /api/v1/notifications/read-all | updated |

Una notificación ajena o inexistente devuelve el mismo 404. Parámetros inválidos:
400. Token inválido/ausente: 401. Almacenamiento no disponible: 503. No se exponen
consultas, contraseñas ni trazas al cliente. Estas respuestas HTTP no ocultan un
error de configuración durante el arranque.

## Pruebas

scripts/run-isolated-tests.py crea su propio cluster PostgreSQL y contenedor
RabbitMQ con puertos/credenciales nuevos; no lee .env ni usa tus servicios. Prueba
outbox, rollback, reintentos, deduplicación, DLQ, API y permisos. Elimina ambos al
terminar. Requiere Docker, PostgreSQL con initdb/pg_ctl, Java 21 y Maven.

```sh
python3 scripts/run-isolated-tests.py
```

Las pruebas de integración se omiten fuera del entorno aislado. Las pruebas de
Flutter usan clientes simulados. Sigue siendo necesaria la prueba manual con tus
dos cuentas y servicios reales para validar tu configuración local.

Referencias: https://www.rabbitmq.com/docs/dlx y
https://docs.spring.io/spring-amqp/reference/amqp/template.html


## Eventos de grupos

Se admiten también estos eventos v1 con targetId de la conversación:

| Tipo | Routing key | Destinatario |
| --- | --- | --- |
| GROUP_MEMBER_ADDED | chat.group.member.added | Cada persona agregada, también al crear el grupo. |
| GROUP_MEMBER_REMOVED | chat.group.member.removed | Persona retirada por la administración. |
| GROUP_ADMIN_CHANGED | chat.group.admin.changed | Nueva persona administradora. |

El binding chat.group.# enruta estos eventos a la misma cola de notificaciones.
Se conservan deduplicación, reintentos y DLQ. Los mensajes grupales usan el evento
CHAT_MESSAGE_CREATED ya existente con fanout a los destinatarios actuales activos.
El consumidor no consulta otra BD para volver a resolver miembros: el productor
autoriza la acción y determina destinatarios en su transacción.

Aplicar database/migrations/002_group_notifications.sql antes de activar productores
de grupos. Reiniciar primero este consumidor y después chat-service. Los mensajes
publicados con tipos nuevos a un consumidor anterior pueden terminar en la DLQ.

La notificación de retiro no abre un grupo inaccesible; solo permite marcarla leída.
Las demás pueden abrir la conversación, cuya API comprueba la membresía actual.
Una notificación histórica no otorga permiso para leer un grupo abandonado.
