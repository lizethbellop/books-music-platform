# Documentación de Controladores REST — `communities-service`

Este documento especifica la arquitectura, convenciones, patrones de autenticación por propagación de cabeceras y casos de uso implementados en la capa de controladores REST (`com.musa.communities_service.controller`).

---

## 1. Arquitectura de la Capa de Controladores

Los controladores REST actúan como el punto de entrada HTTP a la aplicación. Su única responsabilidad es:

1. **Recibir y deserializar** peticiones HTTP (JSON, parámetros de consulta, cabeceras).
2. **Validar la sintaxis y anotaciones** del cuerpo de la petición mediante `@Valid`.
3. **Delegar la ejecución de negocio** a las interfaces de servicio (`CommunityService` y `CommunityActivityService`).
4. **Transformar los resultados** en respuestas `ResponseEntity<T>` estructuradas con códigos de estado HTTP semánticamente correctos (200, 201, 204).

---

## 2. Mecanismo de Propagación de Identidad (`X-User-Id`)

En la arquitectura de microservicios de la plataforma **Musa**, la autenticación y autorización no se procesan directamente en cada microservicio individual. En su lugar, el **API Gateway** realiza la autenticación centralizada y propaga el contexto del usuario autenticado hacia los microservicios mediante la cabecera HTTP personalizada `X-User-Id`.

### 2.1. Flujo de Autenticación en Entorno Integrado (Producción / Gateway Activo)

```
[ Cliente / App Web ]
        │
        │ 1. Request HTTP con Token JWT
        │    Header: Authorization: Bearer eyJhbGciOiJIUzI1...
        ▼
┌────────────────────────────────────────────────────────┐
│                      API Gateway                       │
│  - Valida la firma del Token JWT con el Auth Service   │
│  - Extrae la clave del usuario (UUID) del Claim "sub"  │
│  - Inyecta la cabecera HTTP: X-User-Id                 │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 2. Request reenviada con Header inyectado:
                            │    X-User-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
                            ▼
┌────────────────────────────────────────────────────────┐
│                   communities-service                  │
│  - Recibe: @RequestHeader("X-User-Id") UUID userId     │
│  - Opera directamente con la identidad del usuario     │
└────────────────────────────────────────────────────────┘
```

### 2.2. Flujo de Desarrollo Local / Pruebas Aisladas (Sin Gateway / Postman)

Al ejecutar `communities-service` de forma independiente en entornos locales de pruebas:

* No se requiere iniciar `auth-service` ni el `api-gateway`.
* El desarrollador/tester simula la identidad inyectando directamente el encabezado `X-User-Id` en Postman con UUIDs definidos para las pruebas:
  * **Usuario Admin / Creador:** `11111111-1111-1111-1111-111111111111`
  * **Usuario Miembro:** `22222222-2222-2222-2222-222222222222`
  * **Usuario No Miembro:** `33333333-3333-3333-3333-333333333333`

---

## 3. Matriz de Casos de Uso y Mapeo de Endpoints


| Caso de Uso | Descripción                         | Verbo    | Endpoint                                                    | Permiso / Rol Requerido | Estado HTTP Exitoso |
| :---------- | :----------------------------------- | :------- | :---------------------------------------------------------- | :---------------------- | :------------------ |
| **CU-41**   | Crear Comunidad                      | `POST`   | `/api/v1/communities`                                       | Usuario Autenticado     | `201 Created`       |
| **CU-42**   | Buscar / Listar Comunidades          | `GET`    | `/api/v1/communities`                                       | Usuario Autenticado     | `200 OK`            |
| **CU-43**   | Obtener Comunidad por ID             | `GET`    | `/api/v1/communities/{communityId}`                         | Usuario Autenticado     | `200 OK`            |
| **CU-44**   | Unirse a una Comunidad               | `POST`   | `/api/v1/communities/{communityId}/members`                 | Usuario Autenticado     | `201 Created`       |
| **CU-45**   | Salir de una Comunidad               | `DELETE` | `/api/v1/communities/{communityId}/members/me`              | Miembro Activo          | `204 No Content`    |
| **CU-49**   | Crear Actividad                      | `POST`   | `/api/v1/communities/{communityId}/activities`              | Admin de la Comunidad   | `201 Created`       |
| **CU-50**   | Editar Actividad Programada          | `PUT`    | `/api/v1/communities/{communityId}/activities/{activityId}` | Admin de la Comunidad   | `200 OK`            |
| **CU-51**   | Cancelar / Eliminar Actividad        | `DELETE` | `/api/v1/communities/{communityId}/activities/{activityId}` | Admin de la Comunidad   | `204 No Content`    |
| **CU-52**   | Consultar Agenda de Actividades      | `GET`    | `/api/v1/communities/{communityId}/activities`              | Usuario Autenticado     | `200 OK`            |
| **CU-53**   | Confirmar Asistencia (RSVP)          | `POST`   | `/api/v1/activities/{activityId}/participants`              | Miembro de la Comunidad | `200 OK`            |
| **CU-54**   | Eliminar Comunidad (Borrado Lógico) | `DELETE` | `/api/v1/communities/{communityId}`                         | Admin de la Comunidad   | `204 No Content`    |

---

## 4. Análisis Detallado de Controladores

### 4.1. `CommunityController`

Gestiona las operaciones sobre el ciclo de vida de las comunidades y membresías.

#### A. Creación de Comunidad (`POST /api/v1/communities`)

* **Uso del Header:** Registra al usuario que envía `X-User-Id` como creador de la comunidad (`createdBy`) e inserta automáticamente su membresía con el rol `ADMINISTRADOR`.
* **Respuesta:**

```json
{
  "id": "a7344db4-0e8a-40af-85d4-ea0aeaab3947",
  "name": "Club de Lectura Musa",
  "description": "Comunidad dedicada al análisis literario.",
  "type": "LIBROS",
  "visibility": "PUBLICA",
  "createdBy": "11111111-1111-1111-1111-111111111111",
  "createdAt": "2026-10-29T00:41:53.453196Z",
  "updatedAt": "2026-10-29T00:41:53.453196Z",
  "status": "ACTIVA",
  "memberCount": 1,
  "isMember": true,
  "userRole": "ADMINISTRADOR"
}
```

#### B. Consulta Dinámica de Detalle (`GET /api/v1/communities/{communityId}`)

* **Respuesta contextual por Usuario:** Las propiedades `isMember` y `userRole` cambian en función del valor de `X-User-Id` enviado en la petición:
  * Si consulta el Creador (`1111...`): `"isMember": true, "userRole": "ADMINISTRADOR"`
  * Si consulta un Miembro (`2222...`): `"isMember": true, "userRole": "MIEMBRO"`
  * Si consulta un Usuario ajeno (`3333...`): `"isMember": false, "userRole": null`

---

### 4.2. `CommunityActivityController`

Gestiona el calendario de eventos y el registro de participantes.

#### A. Creación de Actividades (`POST /api/v1/communities/{communityId}/activities`)

* **Validación de Permisos:** El servicio verifica que el `X-User-Id` tenga rol `ADMINISTRADOR` y estado `ACTIVO` en la comunidad objetivo.
* **Respuesta de Error si no es Admin (`403 Forbidden`):**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Se requieren permisos de ADMINISTRADOR para realizar esta acción",
  "path": "/api/v1/communities/a7344db4-0e8a-40af-85d4-ea0aeaab3947/activities",
  "timestamp": "2026-10-29T00:57:56.4595309Z",
  "details": null
}
```

#### B. Confirmación de Asistencia / RSVP (`POST /api/v1/activities/{activityId}/participants`)

* **Validación de Regla de Negocio:** Un usuario solo puede confirmar asistencia a actividades si es miembro activo de la comunidad a la que pertenece el evento. Si no pertenece a la comunidad, la API responde con un `409 Conflict` o `400 Bad Request` informando la restricción.

---

## 5. Anotaciones de Spring y Decisiones Técnicas

1. **`@RestController` + `ResponseEntity<T>`:**

   * Se prefiere el uso explícito de `ResponseEntity.status(...).body(...)` o `ResponseEntity.noContent().build()` frente a la anotación `@ResponseStatus`. Esto otorga flexibilidad total para ajustar dinámicamente cabeceras de respuesta, tipos de retorno y estados HTTP de forma programática.
2. **`@Valid`:**

   * Fuerza la ejecución de la especificación Bean Validation (`jakarta.validation`) sobre los DTOs de entrada (`@NotBlank`, `@Size`, `@NotNull`) antes de invocar la lógica del servicio.
3. **`@PageableDefault`:**

   * Define la paginación por defecto para los endpoints de búsqueda (`size = 10`, ordenamiento predeterminado) en caso de que el cliente no envíe los parámetros `page` y `size`.
4. **Tratamiento Centralizado de Excepciones:**

   * Todas las excepciones de negocio lanzadas desde los controladores o servicios (`ResourceNotFoundException`, `BusinessRuleException`, `ForbiddenActionException`) son interceptadas por `GlobalExceptionHandler` (`@RestControllerAdvice`), retornando un cuerpo `ErrorResponseDto` estandarizado.

---

## 6. Resumen de Códigos de Respuesta HTTP Utilizados


| Código HTTP             | Significado           | Situación en la API                                                                                                     |
| :----------------------- | :-------------------- | :----------------------------------------------------------------------------------------------------------------------- |
| **`200 OK`**             | Operación Exitosa    | Consultas (`GET`), Actualizaciones (`PUT`) y Confirmación de Asistencia (`POST /participants`).                         |
| **`201 Created`**        | Recurso Creado        | Creación exitosa de una comunidad (`POST /communities`), actividad (`POST /activities`) o membresía (`POST /members`). |
| **`204 No Content`**     | Sin Contenido         | Eliminaciones exitosas (`DELETE`) o salida de la comunidad.                                                              |
| **`400 Bad Request`**    | Error de Validación  | El JSON enviado no cumple con las restricciones`@Valid` o el tipo de dato recibido es incorrecto.                        |
| **`403 Forbidden`**      | Acción Prohibida     | El usuario autenticado intenta realizar una acción administrativa sin ser`ADMINISTRADOR`.                               |
| **`404 Not Found`**      | Recurso No Encontrado | El`UUID` de la comunidad o actividad no existe o se encuentra eliminada/cancelada.                                       |
| **`409 Conflict`**       | Conflicto de Negocio  | Nombre de comunidad duplicado o intenciones inconsistentes (ej. fechas finales anteriores a las de inicio).              |
| **`500 Internal Error`** | Error Inesperado      | Captura genérica de excepciones no controladas con log seguro mediante SLF4J.                                           |
