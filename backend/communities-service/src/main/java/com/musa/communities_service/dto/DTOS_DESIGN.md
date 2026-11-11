# Guía de Diseño y Arquitectura de DTOs — `communities-service`

Este documento detalla la estructura, patrones de validación y matriz de trazabilidad entre los **Casos de Uso (CU-41 al CU-54)** y los Objetos de Transferencia de Datos (**DTOs**) de solicitud (`Request`) y respuesta (`Response`).

---

## 1. Principios de Diseño y Buenas Prácticas

1. **Uso de Java `record` (JDK 21):**

   * **Inmutabilidad Nativa:** Los campos son `final` e inmutables por defecto, garantizando que el estado no sea alterado accidentalmente durante la ejecución del request.
   * **Sintaxis Limpia:** Genera automáticamente constructores, `equals()`, `hashCode()` y métodos accesores sin requerir librerías externas.
2. **Estrategia *Fail-Fast* mediante Bean Validation (`jakarta.validation`):**

   * **Protección de la Capa de Negocio:** Las anotaciones (`@NotBlank`, `@NotNull`, `@Size`, `@Future`) interceptan datos inválidos en la capa de controladores (`@Valid`).
   * **Prevención de Errores SQL 500:** Validar en la entrada evita que la base de datos rechace la transacción con excepciones no controladas de PostgreSQL (Constraint Violations).
   * **Retroalimentación Clara al Usuario:** Cada anotación incluye un mensaje descriptivo en español (`message = "..."`) para que el frontend pueda pintar alertas precisas en el formulario sin quedar a ciegas.
3. **Convenciones del Proyecto:**

   * **Sufijo `Dto`:** Todos los archivos de transferencia llevan el sufijo explícito `Dto` (`CommunityCreateRequestDto`, `ActivityResponseDto`) manteniendo coherencia con microservicios vecinos como `users-service`.
   * **Encabezados Javadoc:** Todos los DTOs incluyen un bloque Javadoc en su declaración detallando su propósito funcional.

---

## 2. Matriz de Trazabilidad: Casos de Uso vs DTOs


| Caso de Uso | Operación de Negocio    | Request DTO                 | Response DTO                     | Parámetros de URL / Path                  |
| :---------- | :----------------------- | :-------------------------- | :------------------------------- | :----------------------------------------- |
| **CU-41**   | Crear comunidad          | `CommunityCreateRequestDto` | `CommunityResponseDto`           | N/A                                        |
| **CU-42**   | Buscar comunidad         | N/A (Filtros Query)         | `Page<communityresponsedto>`     | `?query=...&page=0&size=10`                |
| **CU-43**   | Consultar comunidad      | N/A                         | `CommunityResponseDto`           | `/{communityId}`                           |
| **CU-44**   | Unirse a comunidad       | N/A                         | `CommunityMemberResponseDto`     | `/{communityId}/join`                      |
| **CU-45**   | Salir de comunidad       | N/A                         | `CommunityMemberResponseDto`     | `/{communityId}/leave`                     |
| **CU-49**   | Crear actividad          | `ActivityCreateRequestDto`  | `ActivityResponseDto`            | `/{communityId}/activities`                |
| **CU-50**   | Editar actividad         | `ActivityUpdateRequestDto`  | `ActivityResponseDto`            | `/{communityId}/activities/{activityId}`   |
| **CU-51**   | Eliminar actividad       | N/A                         | `void` (HTTP 204)                | `/{communityId}/activities/{activityId}`   |
| **CU-52**   | Consultar actividades    | N/A (Paginación)           | `Page<activityresponsedto>`      | `/{communityId}/activities?page=0&size=10` |
| **CU-53**   | Confirmar participación | N/A                         | `ActivityParticipantResponseDto` | `/activities/{activityId}/confirm`         |
| **CU-54**   | Eliminar comunidad       | N/A                         | `void` (HTTP 204)                | `/{communityId}`                           |

> **Nota Arquitectónica:** Casos de uso como **CU-44 (Unirse)**, **CU-45 (Salir)**, **CU-51 (Eliminar actividad)** o **CU-54 (Eliminar comunidad)** no requieren un `RequestDto` porque los identificadores se transmiten directamente mediante variables de ruta (`@PathVariable`) y el token de autenticación JWT.

---

## 3. Detalle de DTOs de Solicitud (`dto.request`)

### 3.1. `CommunityCreateRequestDto` (CU-41: Crear comunidad)

Representa el formulario de entrada para registrar una comunidad.


| Campo         | Tipo Java              | Anotaciones de Validación      | Propósito y Justificación de la Regla                                                                                                  |
| :------------ | :--------------------- | :------------------------------ | :--------------------------------------------------------------------------------------------------------------------------------------- |
| `name`        | `String`               | `@NotBlank`, `@Size(max = 100)` | Obligatorio. Garantiza que el nombre no esté vacío ni lleno de espacios en blanco, respetando el tamaño de columna SQL`VARCHAR(100)`. |
| `description` | `String`               | `@Size(max = 1000)`             | Opcional. Limita la biografía al máximo de caracteres permitido por la base de datos.                                                  |
| `type`        | `TipoComunidad`        | `@NotNull`                      | Obligatorio. Define la categoría (`LIBROS`, `MUSICA`, `MIXTO`).                                                                         |
| `visibility`  | `VisibilidadComunidad` | `@NotNull`                      | Obligatorio. Determina si la comunidad es`PUBLICA` o `PRIVADA`.                                                                          |

---

### 3.2. `CommunityUpdateRequestDto` (Editar comunidad)

Representa los campos editables de una comunidad existente.


| Campo         | Tipo Java              | Anotaciones de Validación | Propósito y Justificación de la Regla                                  |
| :------------ | :--------------------- | :------------------------- | :----------------------------------------------------------------------- |
| `description` | `String`               | `@Size(max = 1000)`        | Permite actualizar la biografía asegurando la restricción de longitud. |
| `type`        | `TipoComunidad`        | Ninguna (Opcional)         | Permite la reconfiguración del tipo de grupo.                           |
| `visibility`  | `VisibilidadComunidad` | Ninguna (Opcional)         | Permite alternar la visibilidad entre pública y privada.                |

---

### 3.3. `ActivityCreateRequestDto` (CU-49: Crear actividades)

Formulario para la programación de eventos dentro de una comunidad.


| Campo         | Tipo Java        | Anotaciones de Validación      | Propósito y Justificación de la Regla                                                |
| :------------ | :--------------- | :------------------------------ | :------------------------------------------------------------------------------------- |
| `title`       | `String`         | `@NotBlank`, `@Size(max = 150)` | Obligatorio. Evita títulos vacíos y valida la longitud física SQL`VARCHAR(150)`.    |
| `description` | `String`         | `@Size(max = 1000)`             | Opcional. Detalle de la actividad.                                                     |
| `location`    | `String`         | `@Size(max = 255)`              | Opcional. Enlace virtual o dirección física del evento.                              |
| `startsAt`    | `OffsetDateTime` | `@NotNull`, `@Future`           | Obligatorio. Exige que el evento sea programado para una fecha y hora futura.          |
| `endsAt`      | `OffsetDateTime` | Ninguna                         | Opcional. La validez lógica (`endsAt > startsAt`) se gestiona en la capa de servicio. |

---

### 3.4. `ActivityUpdateRequestDto` (CU-50: Editar actividades)

Formulario para modificar una actividad en estado `PROGRAMADA`.


| Campo         | Tipo Java        | Anotaciones de Validación      | Propósito y Justificación de la Regla |
| :------------ | :--------------- | :------------------------------ | :-------------------------------------- |
| `title`       | `String`         | `@NotBlank`, `@Size(max = 150)` | Obligatorio.                            |
| `description` | `String`         | `@Size(max = 1000)`             | Opcional.                               |
| `location`    | `String`         | `@Size(max = 255)`              | Opcional.                               |
| `startsAt`    | `OffsetDateTime` | Ninguna                         | Nueva fecha propuesta para el evento.   |
| `endsAt`      | `OffsetDateTime` | Ninguna                         | Nueva fecha de finalización propuesta. |

---

## 4. Detalle de DTOs de Respuesta (`dto.response`)

### 4.1. `CommunityResponseDto` (CU-42, CU-43)

Objeto de salida completo expuesto hacia el frontend.


| Campo             | Tipo Java              | Descripción y Lógica Calculada                                                   |
| :---------------- | :--------------------- | :--------------------------------------------------------------------------------- |
| `id`              | `UUID`                 | Identificador único de la comunidad.                                              |
| `name`            | `String`               | Nombre de la comunidad.                                                            |
| `description`     | `String`               | Descripción detallada.                                                            |
| `type`            | `TipoComunidad`        | Categoria (`LIBROS`, `MUSICA`, `MIXTO`).                                           |
| `visibility`      | `VisibilidadComunidad` | Nivel de acceso (`PUBLICA`, `PRIVADA`).                                            |
| `createdBy`       | `UUID`                 | UUID del usuario creador.                                                          |
| `createdAt`       | `OffsetDateTime`       | Marca de tiempo UTC de creación.                                                  |
| `updatedAt`       | `OffsetDateTime`       | Marca de tiempo UTC de última actualización.                                     |
| `status`          | `EstadoComunidad`      | Estado lógico (`ACTIVA`, `ELIMINADA`).                                            |
| **`memberCount`** | `long`                 | **Campo Calculado:** Cantidad total de miembros en estado `ACTIVO`.                |
| **`isMember`**    | `boolean`              | **Campo Calculado:** Indica si el usuario autenticado pertenece al grupo.          |
| **`userRole`**    | `RolMiembro`           | **Campo Calculado:** Rol del usuario actual (`ADMINISTRADOR`, `MIEMBRO` o `null`). |

---

### 4.2. `CommunityMemberResponseDto` (CU-43, CU-44, CU-45)

Respuesta para representar la membresía individual de un usuario.


| Campo         | Tipo Java        | Descripción                                    |
| :------------ | :--------------- | :---------------------------------------------- |
| `id`          | `UUID`           | Identificador del registro de membresía.       |
| `communityId` | `UUID`           | Identificador de la comunidad asociada.         |
| `userId`      | `UUID`           | Identificador del usuario miembro.              |
| `role`        | `RolMiembro`     | Rol otorgado (`ADMINISTRADOR`, `MIEMBRO`).      |
| `joinedAt`    | `OffsetDateTime` | Fecha y hora en la que se unió al grupo.       |
| `status`      | `EstadoMiembro`  | Estado de la membresía (`ACTIVO`, `RETIRADO`). |

---

### 4.3. `ActivityResponseDto` (CU-52)

Respuesta detallada para la agenda de eventos.


| Campo                  | Tipo Java         | Descripción y Lógica Calculada                                           |
| :--------------------- | :---------------- | :------------------------------------------------------------------------- |
| `id`                   | `UUID`            | Identificador único de la actividad.                                      |
| `communityId`          | `UUID`            | Identificador de la comunidad organizadora.                                |
| `createdBy`            | `UUID`            | UUID del creador de la actividad.                                          |
| `title`                | `String`          | Título del evento.                                                        |
| `description`          | `String`          | Detalle o temario de la actividad.                                         |
| `location`             | `String`          | Lugar físico o enlace web.                                                |
| `startsAt`             | `OffsetDateTime`  | Fecha y hora de inicio.                                                    |
| `endsAt`               | `OffsetDateTime`  | Fecha y hora de finalización.                                             |
| `createdAt`            | `OffsetDateTime`  | Marca de tiempo de creación.                                              |
| `updatedAt`            | `OffsetDateTime`  | Marca de tiempo de edición.                                               |
| `status`               | `EstadoActividad` | Estado del evento (`PROGRAMADA`, `CANCELADA`, `COMPLETADA`).               |
| **`participantCount`** | `long`            | **Campo Calculado:** Total de asistentes confirmados.                      |
| **`isParticipating`**  | `boolean`         | **Campo Calculado:** Indica si el usuario actual ha confirmado asistencia. |

---

### 4.4. `ActivityParticipantResponseDto` (CU-53)

Respuesta para la confirmación de asistencia (RSVP).


| Campo         | Tipo Java             | Descripción                                          |
| :------------ | :-------------------- | :---------------------------------------------------- |
| `id`          | `UUID`                | Identificador del registro de participación.         |
| `activityId`  | `UUID`                | Identificador de la actividad.                        |
| `userId`      | `UUID`                | Identificador del usuario asistente.                  |
| `status`      | `EstadoParticipacion` | Estado de participación (`CONFIRMADA`, `CANCELADA`). |
| `confirmedAt` | `OffsetDateTime`      | Fecha y hora UTC en que confirmó la asistencia.      |
