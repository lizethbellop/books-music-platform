# JUnit de communities-service

Todas las pruebas están escritas en Java con JUnit Jupiter, bajo `src/test/java`. Maven Surefire las ejecuta. Mockito se utiliza para simular conexiones WebSocket lentas; PostgreSQL y RabbitMQ reales y temporales se utilizan en las pruebas de integración.

## Ejecutar solamente las pruebas unitarias de conexiones e hilos

Desde `backend/communities-service`:

```bash
./mvnw -Dtest=PollWebSocketBoundsTest test
```

No necesita PostgreSQL ni RabbitMQ. También puede ejecutarse esta clase con el botón de JUnit en IntelliJ.

## Ejecutar todas las pruebas del servicio

Desde la raíz de `books-music-platform`:

```bash
python3 backend/communities-service/scripts/run-isolated-tests.py
```

El auxiliar prepara infraestructura aislada y llama a `mvn test` únicamente para `communities-service`. No contiene las aserciones ni ejecuta las pruebas de otros microservicios. Crea una sola base temporal de comunidades y un broker RabbitMQ temporal; los elimina al terminar. No usa los `.env` ni las bases locales habituales.

Requisitos: Java 21, Maven, Python 3, Docker encendido, imagen local `rabbitmq:4-management` y PostgreSQL con `initdb`/`pg_ctl` (detecta Postgres.app en macOS o los binarios en PATH).

Para ejecutar solo los casos marcados como concurrencia, con la misma infraestructura temporal:

```bash
python3 backend/communities-service/scripts/run-isolated-tests.py -Dgroups=concurrency
```

Las pruebas de integración tienen una protección adicional: requieren `POLLS_TEST_ISOLATED=true` y comprueban la cuenta `integration_owner` y que el puerto de PostgreSQL no sea 5432 antes de limpiar datos. El auxiliar configura estas variables; sin la infraestructura las pruebas de integración de encuestas quedan omitidas, por lo que ejecutar únicamente `mvn test` sin prepararla no equivale a comprobar toda la integración.

## Archivos de encuestas

### PollBackendIntegrationTest.java

Ubicación: `src/test/java/com/musa/communities_service/polls/`.

Pruebas HTTP con servidor real, PostgreSQL real y RabbitMQ real, todos aislados. Incluye:

- JWT obligatorio, firma, emisor, audiencia, vencimiento y sujeto UUID.
- Roles, membresía activa, acceso entre comunidades y tickets.
- Datos inválidos, opciones de otra encuesta, referencias SQL y paginación.
- Voto único, repetición idempotente y rechazo del cambio de opción.
- Doce solicitudes simultáneas de un mismo usuario: persiste un voto.
- Veinticuatro usuarios, ocho trabajadores: se cuentan todos los votos confirmados.
- Dos opciones simultáneas del mismo usuario: una solicitud tiene éxito y otra obtiene conflicto.
- Cierre que espera un bloqueo compartido de votación; los votos nuevos se rechazan después.
- Espera de bloqueos observada en `pg_stat_activity`, con plazo máximo para que la prueba no se quede esperando.
- Una encuesta bloqueada no detiene los votos sobre otra encuesta.
- Cancelación de un trabajador: revierte el voto y libera el bloqueo para cerrar la encuesta.
- Timeout de bloqueo con respuesta 503 y sin voto parcial.
- Agotamiento del pool con respuesta 503 y recuperación al liberar conexiones.
- Atomicidad del voto y su aviso pendiente.
- Fallo de conexión del publicador al broker y recuperación posterior.
- Preservación de un cambio nuevo durante la confirmación de uno anterior.
- Tamaño máximo del cuerpo HTTP y límite de frecuencia.
- WebSocket autenticado con ticket de un uso, ping/pong, aviso del broker y cierre por falta de autenticación.

Los trabajadores creados por los casos de concurrencia se detienen en `finally` y se comprueba su terminación con un plazo máximo.

### PollWebSocketBoundsTest.java

Ubicación: `src/test/java/com/musa/communities_service/polls/realtime/`.

JUnit sin contexto Spring ni infraestructura externa. Cubre:

- Rechazo de la conexión 201.
- Rechazo del cuarto socket autenticado de un mismo usuario.
- Doscientos clientes lentos: se esperan cuatro trabajadores realmente activos, se verifica que no se supere ese máximo y que la cola llena rechace trabajo.
- Ausencia de dos envíos simultáneos sobre una misma conexión.
- Drenaje de la cola y aceptación de una conexión sana después de la saturación.
- Apagado que interrumpe los envíos bloqueados y cierra las conexiones.
- Rechazo de tickets inválidos.

Las coordinaciones de hilos usan `CountDownLatch`, esperas acotadas y `@Timeout`; no dependen de dejar dormida la prueba para suponer que una operación ya empezó.

## Alcance

Las pruebas anteriores de controladores, servicios, repositorios y mappers de comunidades se conservan. Solo se corrigió la fecha fija vencida de una prueba de actividades para calcular una fecha futura; no se alteró su lógica de negocio.

Se retiraron las dos clases de pruebas de encuestas que se habían agregado al gateway. Se conservó su clase anterior `ChatGatewayRoutesTest`. El auxiliar ya no prepara bases ni ejecuta suites de chat, social, gateway o notificaciones.

Los reportes JUnit de este servicio se generan en `target/surefire-reports`. Las comprobaciones de límites de hilos/colas y concurrencia son funcionales; no equivalen a una medición de memoria ni a un ensayo de carga prolongado en producción.
