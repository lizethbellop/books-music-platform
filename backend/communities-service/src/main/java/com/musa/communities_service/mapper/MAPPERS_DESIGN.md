# Guía de Arquitectura de la Capa de Mapeo (Mappers) — `communities-service`

## 1. Propósito y Justificación Técnica

La capa de Mappers actúa como el puente de transformación inmutable entre el **Modelo de Datos Persistente (`Entity`)** y los **Objetos de Transferencia de Datos (`DTO`)**.

### ¿Por qué utilizar Mappers dedicados (`@Component`)?

1. **Principio de Responsabilidad Única (SRP):**

   * Desacopla la capa de servicios (`ServiceImpl`) de la lógica repetitiva de conversión de objetos, permitiendo que los servicios se concentren exclusivamente en transacciones, reglas de negocio y control de permisos.
2. **Aislamiento de Entidades JPA:**

   * Evita la exposición de entidades de persistencia hacia los controladores REST o el cliente frontend.
   * Previene excepciones de tipo `LazyInitializationException` al acceder a relaciones `@ManyToOne` o `@OneToMany` fuera de una transacción `@Transactional`.
   * Corta bucles de referencia circular durante la serialización JSON.
3. **Cálculo de Banderas y Metadatos Contextuales:**

   * Las entidades en la base de datos almacenan un estado estático (ej. una fila en `communities`). Sin embargo, los DTOs de respuesta entregan datos dinámicos que dependen de la consulta y del usuario autenticado (`memberCount`, `isMember`, `userRole`, `participantCount`, `isParticipating`).
   * El Mapper recibe estas variables calculadas por el servicio y las ensambla en una única estructura inmutable (`record`).

---

## 2. Desglose de Componentes

### 2.1. `CommunityMapper`

Transforma la información relacionada con comunidades y sus membresías.

#### Métodos de Conversión


| Método               | Entrada                                                                           | Salida                       | Propósito y Lógica Interna                                                                                                                     |
| :-------------------- | :-------------------------------------------------------------------------------- | :--------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------- |
| `toEntity`            | `CommunityCreateRequestDto`, `UUID createdBy`                                     | `Community`                  | Construye una nueva entidad`Community` en estado inicial `ACTIVA` asociando el ID del usuario creador.                                           |
| `toResponseDto`       | `Community entity`, `long memberCount`, `boolean isMember`, `MemberRole userRole` | `CommunityResponseDto`       | Mapea todos los atributos persistidos de la comunidad e inyecta el total de miembros activos y la relación del usuario actual con la comunidad. |
| `toMemberResponseDto` | `CommunityMember member`                                                          | `CommunityMemberResponseDto` | Extrae la relación de membresía extrayendo de forma segura el`UUID` de la comunidad vinculada (`member.getCommunity().getId()`).               |

#### Mapeo Detallado de Atributos (`CommunityResponseDto`)


| Atributo DTO      | Fuente / Origen           | Tipo de Dato          | Descripción                                                       |
| :---------------- | :------------------------ | :-------------------- | :----------------------------------------------------------------- |
| `id`              | `entity.getId()`          | `UUID`                | Identificador único de la comunidad.                              |
| `name`            | `entity.getName()`        | `String`              | Nombre de la comunidad.                                            |
| `description`     | `entity.getDescription()` | `String`              | Descripción detallada.                                            |
| `type`            | `entity.getType()`        | `CommunityType`       | Tipo de contenido (LIBROS, MUSICA, MIXTO).                         |
| `visibility`      | `entity.getVisibility()`  | `CommunityVisibility` | Alcance (PUBLICA, PRIVADA).                                        |
| `createdBy`       | `entity.getCreatedBy()`   | `UUID`                | ID del usuario creador.                                            |
| `createdAt`       | `entity.getCreatedAt()`   | `OffsetDateTime`      | Fecha de creación en UTC.                                         |
| `updatedAt`       | `entity.getUpdatedAt()`   | `OffsetDateTime`      | Fecha de última modificación en UTC.                             |
| `status`          | `entity.getStatus()`      | `CommunityStatus`     | Estado del ciclo de vida (ACTIVA, ELIMINADA).                      |
| **`memberCount`** | Calculado en servicio     | `long`                | Total de miembros activos registrados en la comunidad.             |
| **`isMember`**    | Calculado en servicio     | `boolean`             | Indica si el usuario que consulta pertenece a la comunidad.        |
| **`userRole`**    | Calculado en servicio     | `MemberRole`          | Rol del usuario consultante (`ADMINISTRADOR`, `MIEMBRO` o `null`). |

---

### 2.2. `ActivityMapper`

Transforma la información de la agenda de eventos y confirmaciones de asistencia.

#### Métodos de Conversión


| Método                    | Entrada                                                                        | Salida                           | Propósito y Lógica Interna                                                                                                 |
| :------------------------- | :----------------------------------------------------------------------------- | :------------------------------- | :--------------------------------------------------------------------------------------------------------------------------- |
| `toEntity`                 | `ActivityCreateRequestDto`, `Community community`, `UUID createdBy`            | `CommunityActivity`              | Instancia la entidad vinculando el objeto entidad`Community` (relación `@ManyToOne`) y asignando el estado `PROGRAMADA`.    |
| `toResponseDto`            | `CommunityActivity entity`, `long participantCount`, `boolean isParticipating` | `ActivityResponseDto`            | Transforma la actividad extraída de BD a DTO, extrayendo el`UUID` de la comunidad y adjuntando métricas de participación. |
| `toParticipantResponseDto` | `ActivityParticipant participant`                                              | `ActivityParticipantResponseDto` | Convierte el registro de asistencia RSVP extrayendo el`UUID` de la actividad asociada.                                       |

#### Mapeo Detallado de Atributos (`ActivityResponseDto`)


| Atributo DTO           | Fuente / Origen                 | Tipo de Dato     | Descripción                                                  |
| :--------------------- | :------------------------------ | :--------------- | :------------------------------------------------------------ |
| `id`                   | `entity.getId()`                | `UUID`           | Identificador único de la actividad.                         |
| `communityId`          | `entity.getCommunity().getId()` | `UUID`           | Identificador de la comunidad organizadora.                   |
| `createdBy`            | `entity.getCreatedBy()`         | `UUID`           | ID del usuario organizador.                                   |
| `title`                | `entity.getTitle()`             | `String`         | Título del evento.                                           |
| `description`          | `entity.getDescription()`       | `String`         | Descripción detallada.                                       |
| `location`             | `entity.getLocation()`          | `String`         | Ubicación física o enlace virtual.                          |
| `startsAt`             | `entity.getStartsAt()`          | `OffsetDateTime` | Fecha y hora de inicio UTC.                                   |
| `endsAt`               | `entity.getEndsAt()`            | `OffsetDateTime` | Fecha y hora de finalización UTC (opcional).                 |
| `createdAt`            | `entity.getCreatedAt()`         | `OffsetDateTime` | Fecha de creación en UTC.                                    |
| `updatedAt`            | `entity.getUpdatedAt()`         | `OffsetDateTime` | Fecha de última modificación en UTC.                        |
| `status`               | `entity.getStatus()`            | `ActivityStatus` | Estado del evento (PROGRAMADA, CANCELADA).                    |
| **`participantCount`** | Calculado en servicio           | `long`           | Cantidad total de usuarios con asistencia confirmada.         |
| **`isParticipating`**  | Calculado en servicio           | `boolean`        | Indica si el usuario autenticado ha confirmado su asistencia. |

---

## 3. Manejo de Relaciones y Nulabilidad

Para garantizar la robustez del código y evitar `NullPointerException`:

1. **Extracción de Claves Foráneas (`@ManyToOne`):**
   * Al convertir de entidad a DTO de respuesta, se accede a los identificadores mediante invocaciones directas a los métodos getter de la relación (ej. `entity.getCommunity() != null ? entity.getCommunity().getId() : null`).
2. **Inmutabilidad:**
   * Todos los métodos de los mappers son puros y no modifican el estado de las entidades pasadas por parámetro.
