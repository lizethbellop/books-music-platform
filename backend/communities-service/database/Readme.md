# Database Schema & Data Dictionary — `communities_db`

Este directorio contiene los scripts DDL y la documentación del modelo relacional de la base de datos PostgreSQL correspondiente al microservicio `communities-service`.



## Diagrama de Relaciones (ER Overview)

* **`communities` (1) ─── (N) `community_members`**: Una comunidad agrupa múltiples miembros.
* **`communities` (1) ─── (N) `community_activities`**: Una comunidad organiza eventos o actividades.
* **`community_activities` (1) ─── (N) `activity_participants`**: Una actividad registra participantes confirmados o cancelados.

---

## Diccionario de Datos

### 1. Tabla: `communities`
Almacena la información principal de cada comunidad de lectura, música o temática mixta.

| Columna | Tipo SQL | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | **PK**, NOT NULL | Identificador único universal de la comunidad. |
| `name` | `VARCHAR(100)` | NOT NULL, CHECK (`trim > 0`) | Nombre oficial de la comunidad. |
| `description` | `VARCHAR(1000)` | NULL | Descripción o biografía de la comunidad. |
| `type` | `VARCHAR(20)` | NOT NULL, CHECK | Tipo de comunidad: `LIBROS`, `MUSICA`, `MIXTO`. |
| `visibility` | `VARCHAR(20)` | NOT NULL, CHECK | Nivel de visibilidad: `PUBLICA`, `PRIVADA`. |
| `created_by` | `UUID` | NOT NULL | UUID del usuario creador (asociado a `user-service`). |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Fecha y hora de creación de la comunidad. |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Última fecha de actualización del registro. |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'ACTIVA'` | Estado lógico: `ACTIVA`, `ELIMINADA`. |

---

### 2. Tabla: `community_members`
Tabla intermedia que gestiona la relación entre los usuarios de la plataforma y las comunidades a las que pertenecen.

| Columna | Tipo SQL | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | **PK**, NOT NULL | Identificador único del registro de membresía. |
| `community_id` | `UUID` | **FK** (`communities.id`), NOT NULL | Comunidad a la que pertenece el miembro (ON DELETE CASCADE). |
| `user_id` | `UUID` | NOT NULL | UUID del usuario (asociado a `user-service`). |
| `role` | `VARCHAR(20)` | NOT NULL, CHECK | Rol dentro del grupo: `ADMINISTRADOR`, `MIEMBRO`. |
| `joined_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Fecha y hora de unión al grupo. |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'ACTIVO'` | Estado de la membresía: `ACTIVO`, `RETIRADO`. |

* **Constraint Único**: `UNIQUE (community_id, user_id)` — Impide que un mismo usuario se registre dos veces en la misma comunidad.

---

### 3. Tabla: `community_activities`
Registra los eventos, lecturas conjuntas, sesiones escuchadas o actividades programadas en una comunidad.

| Columna | Tipo SQL | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | **PK**, NOT NULL | Identificador único de la actividad. |
| `community_id` | `UUID` | **FK** (`communities.id`), NOT NULL | Comunidad donde se organiza la actividad (ON DELETE CASCADE). |
| `created_by` | `UUID` | NOT NULL | UUID del usuario que organizó la actividad. |
| `title` | `VARCHAR(150)` | NOT NULL, CHECK (`trim > 0`) | Título o nombre de la actividad. |
| `description` | `VARCHAR(1000)` | NULL | Detalle o temario del evento. |
| `location` | `VARCHAR(255)` | NULL | Enlace virtual (Zoom/Meet) o ubicación física. |
| `starts_at` | `TIMESTAMPTZ` | NOT NULL | Fecha y hora de inicio de la actividad. |
| `ends_at` | `TIMESTAMPTZ` | NULL, CHECK (`ends_at > starts_at`) | Fecha y hora de finalización de la actividad. |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Fecha y hora de creación de la actividad. |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Última modificación de la actividad. |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'PROGRAMADA'` | Estado de la actividad: `PROGRAMADA`, `CANCELADA`, `COMPLETADA`. |

---

### 4. Tabla: `activity_participants`
Registra las asistencias / RSVPs de los usuarios a las actividades organizadas.

| Columna | Tipo SQL | Restricciones | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | **PK**, NOT NULL | Identificador único de la inscripción. |
| `activity_id` | `UUID` | **FK** (`community_activities.id`), NOT NULL | Actividad a la que se asiste (ON DELETE CASCADE). |
| `user_id` | `UUID` | NOT NULL | UUID del usuario participante. |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'CONFIRMADA'` | Estado de la participación: `CONFIRMADA`, `CANCELADA`. |
| `confirmed_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `NOW()` | Fecha y hora de confirmación de asistencia. |

* **Constraint Único**: `UNIQUE (activity_id, user_id)` — Un usuario solo puede inscribirse una vez por actividad.

---

## Índices de Rendimiento

| Nombre del Índice | Tabla | Columnas | Objetivo |
| :--- | :--- | :--- | :--- |
| `idx_communities_name` | `communities` | `(name)` | Búsquedas rápidas y autocompletado por nombre. |
| `idx_communities_created_by` | `communities` | `(created_by)` | Consultar comunidades creadas por un usuario específico. |
| `idx_members_user_status` | `community_members` | `(user_id, status)` | Listar las comunidades activas a las que pertenece un usuario. |
| `idx_members_community_status`| `community_members` | `(community_id, status)` | Listar los miembros activos de una comunidad. |
| `idx_activities_community_start`| `community_activities` | `(community_id, starts_at)` | Calendario y feed de eventos ordenados cronológicamente por comunidad. |
| `idx_participants_user` | `activity_participants` | `(user_id)` | Historial de actividades en las que participa un usuario. |

---

## Guía para el Mapeo a Entidades JPA (Spring Boot 3 / JDK 21)

Al momento de implementar las clases `@Entity`:

1. **UUIDs**: Utilizar `java.util.UUID` mapeado con `@Id` y `@GeneratedValue(strategy = GenerationType.UUID)`.
2. **Fechas/Tiempos (`TIMESTAMPTZ`)**: Usar `java.time.OffsetDateTime` o `java.time.Instant`.
3. **Enumeraciones Java:**:
    * `TipoComunidad:CommunityType` (`LIBROS`, `MUSICA`, `MIXTO`)
    * `VisibilidadComunidad:CommunityVisibility` (`PUBLICA`, `PRIVADA`)
    * `EstadoComunidad:CommunityStatus` (`ACTIVA`, `ELIMINADA`)
    * `RolMiembro:MemberRole` (`ADMINISTRADOR`, `MIEMBRO`)
    * `EstadoMiembro:MemberStatus` (`ACTIVO`, `RETIRADO`)
    * `EstadoActividad:ActivityStatus` (`PROGRAMADA`, `CANCELADA`, `COMPLETADA`)
    * `EstadoParticipacion:ParticipantStatus` (`CONFIRMADA`, `CANCELADA`)
      *(Mapear siempre con `@Enumerated(EnumType.STRING)`)*.
4. **Relaciones FK**: Utilizar `@ManyToOne(fetch = FetchType.LAZY)` en las clases hijas apuntando a sus entidades padre (`Comunidad` o `ActividadComunidad`).