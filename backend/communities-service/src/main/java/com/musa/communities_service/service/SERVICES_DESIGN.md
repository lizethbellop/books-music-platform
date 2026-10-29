# ⚙️ Guía de Arquitectura y Diseño de la Capa de Servicios — `SERVICES_DESIGN.md`

Este documento define la arquitectura, los patrones de diseño y la implementación detallada de la capa de servicios (`com.musa.communities_service.service`) dentro del microservicio `communities-service`. Cubre los contratos de interfaz, las reglas de negocio, la gestión de transacciones JPA y la trazabilidad de los casos de uso (**CU-41 al CU-54**).

---

## 1. Principios de Diseño y Patrones Arquitectónicos

La capa de servicios opera como el núcleo de orquestación del microservicio, aplicando los siguientes principios:

### 1.1. Inversión de Dependencias (DIP)
Se separan estrictamente las interfaces de contrato (`CommunityService`, `CommunityActivityService`) de sus implementaciones concretas (`CommunityServiceImpl`, `CommunityActivityServiceImpl`).
* **Pruebas Unitarias:** Permite la sustitución de dependencias mediante Mocks (`Mockito`) sin levantar la infraestructura de Spring.
* **Extensibilidad:** Facilita cambiar la estrategia de implementación sin alterar la capa de presentación (controladores REST).

### 1.2. Aislamiento del Modelo de Dominio (Uso Exclusivo de DTOs)
Los servicios **nunca reciben ni retornan entidades JPA (`Entity`)**. Toda interacción se realiza mediante *Records* DTO (`*RequestDto` y `*ResponseDto`).
* **Prevención de `LazyInitializationException`:** Evita accesos a colecciones o relaciones `@ManyToOne` / `@OneToMany` proxy fuera de una transacción activa.
* **Eliminación de Bucles de Serialización:** Previene referencias circulares en el árbol JSON durante el renderizado HTTP.
* **Seguridad de Esquema:** Impide exponer la estructura interna de las tablas PostgreSQL hacia capas externas.

### 1.3. Inyección Explícita de Contexto de Seguridad (`currentUserId`)
Cada operación que requiera validación de permisos o registro de auditoría recibe de forma explícita el parámetro `UUID currentUserId`, extraído de la cabecera HTTP `X-User-Id` por el controlador REST.
* La capa de servicio es completamente responsable de validar si el usuario autenticado tiene permisos suficientes (ej. rol de `ADMINISTRADOR`) para ejecutar comandos mutativos.

### 1.4. Gestión Transaccional Fina (`@Transactional`)
* **Lecturas optimizadas:** Consultas anotadas con `@Transactional(readOnly = true)` instruyen a Hibernate para deshabilitar la inspección de cambios (*dirty checking*), optimizando el uso de memoria y acelerando la ejecución en PostgreSQL.
* **Consistencia atómica:** Métodos mutativos encapsulan múltiples escrituras en una sola transacción (ej. al crear una comunidad se persiste el registro en `communities` e inmediatamente la membresía del creador en `community_members`).

---

## 2. Matriz de Mapeo de Casos de Uso (CU-41 al CU-54)

### Módulo de Comunidades y Membresías (`CommunityService`)

| Caso de Uso | Operación de Negocio | Método en la Interfaz | Tipo de Retorno | Reglas de Negocio Aplicadas |
| :--- | :--- | :--- | :--- | :--- |
| **CU-41** | Crear comunidad | `createCommunity(request, currentUserId)` | `CommunityResponseDto` | Valida unicidad de nombre. Persiste comunidad y registra automáticamente al creador con rol `ADMINISTRADOR` y estado `ACTIVO`. |
| **CU-42** | Buscar comunidades | `searchCommunities(query, type, pageable, currentUserId)` | `Page<CommunityResponseDto>` | Ejecuta búsqueda JPQL con coincidencias parciales (`LIKE`) en estado `ACTIVA`. Calcula dinámicamente `memberCount`, `isMember` y `userRole`. |
| **CU-43** | Consultar comunidad | `getCommunityById(communityId, currentUserId)` | `CommunityResponseDto` | Valida existencia en estado `ACTIVA`. Retorna la entidad mapeada con métricas contextuales relativas al usuario solicitante. |
| **N/A** | Editar comunidad | `updateCommunity(communityId, request, currentUserId)` | `CommunityResponseDto` | Valida rol `ADMINISTRADOR`. Actualiza únicamente los campos presentes en el DTO (descripción, tipo, visibilidad). |
| **CU-44** | Unirse a comunidad | `joinCommunity(communityId, currentUserId)` | `CommunityMemberResponseDto` | Si el usuario ya fue miembro y estaba `RETIRADO`, reactiva su membresía a `ACTIVO`; si es la primera vez, crea el registro con rol `MIEMBRO`. |
| **CU-45** | Salir de comunidad | `leaveCommunity(communityId, currentUserId)` | `void` | Cambia estado a `RETIRADO`. Lanza `BusinessRuleException` si el usuario intenta salir siendo el único `ADMINISTRADOR` activo. |
| **CU-54** | Eliminar comunidad | `deleteCommunity(communityId, currentUserId)` | `void` | Borrado lógico: Actualiza el estado de la comunidad a `ELIMINADA`. Requiere permisos de `ADMINISTRADOR`. |

---

### Módulo de Actividades y Asistencia (`CommunityActivityService`)

| Caso de Uso | Operación de Negocio | Método en la Interfaz | Tipo de Retorno | Reglas de Negocio Aplicadas |
| :--- | :--- | :--- | :--- | :--- |
| **CU-49** | Crear actividad | `createActivity(communityId, request, currentUserId)` | `ActivityResponseDto` | Requiere permisos de `ADMINISTRADOR`. Valida que `endsAt` no sea anterior a `startsAt`. Asigna estado inicial `PROGRAMADA`. |
| **CU-50** | Editar actividad | `updateActivity(communityId, activityId, request, currentUserId)` | `ActivityResponseDto` | Requiere rol `ADMINISTRADOR`. Valida que el evento esté `PROGRAMADA` y verifica la coherencia de fechas modificadas. |
| **CU-51** | Cancelar actividad | `deleteActivity(communityId, activityId, currentUserId)` | `void` | Borrado lógico: Modifica el estado de la actividad a `CANCELADA`. Requiere rol `ADMINISTRADOR`. |
| **CU-52** | Consultar agenda | `getCommunityActivities(communityId, pageable, currentUserId)` | `Page<ActivityResponseDto>` | Filtra actividades en estado `PROGRAMADA` paginadas. Inyecta `participantCount` total e `isParticipating` para el usuario solicitante. |
| **CU-53** | Confirmar asistencia | `confirmAttendance(activityId, currentUserId)` | `ActivityParticipantResponseDto` | Requiere que el usuario sea miembro `ACTIVO` de la comunidad organizadora. Registra o actualiza el registro a `CONFIRMADA`. |

---

## 3. Justificación Técnica: Firmas y Tipos de Retorno

### 3.1. Uso de `Page<DTO>` vs. `List<DTO>`

Cuando los endpoints de consulta procesan grandes volúmenes de datos, retornar colecciones simples (`List<T>`) degrada el rendimiento de la aplicación por dos motivos:
1. **Consumo Excesivo de Memoria RAM:** Carga en memoria la totalidad de los registros de la base de datos de una sola vez.
2. **Payloads JSON Masivos:** Transfiere respuestas de múltiples megabytes por la red hacia el cliente o API Gateway.

Al utilizar `Page<T>` apoyado por Spring Data JPA, las consultas SQL generadas en PostgreSQL aplican cláusulas `LIMIT` y `OFFSET` a nivel de base de datos. El servicio transforma cada elemento dentro de la página mediante el método `.map(...)`:

```json
{
  "content": [
    {
      "id": "a7344db4-0e8a-40af-85d4-ea0aeaab3947",
      "name": "Club de Lectura Musa",
      "memberCount": 2,
      "isMember": true,
      "userRole": "ADMINISTRADOR"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### 3.2. Retornos `void` para Comandos Destructivos
Métodos como `leaveCommunity`, `deleteCommunity` y `deleteActivity` retornan `void`. En la arquitectura REST, la ejecución exitosa de un comando que no requiere la devolución de datos se traduce en un código de respuesta **`HTTP 204 No Content`**, optimizando el ancho de banda.

---

## 4. Análisis de Implementación de Reglas de Negocio

### 4.1. `CommunityServiceImpl`

#### Control de Administración Única en Salidas (CU-45)
Garantiza que una comunidad activa nunca quede huérfana de administradores:

```java
if (member.getRole() == MemberRole.ADMINISTRADOR) {
    long activeAdmins = memberRepository.countByCommunityIdAndRoleAndStatus(
            communityId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
    if (activeAdmins <= 1) {
        throw new BusinessRuleException("No puedes salir de la comunidad siendo el único administrador");
    }
}
```

#### Idempotencia y Reactivación de Membresías (CU-44)
Previene duplicidad de filas en la tabla `community_members`. Si un usuario reingresa tras haberse retirado, actualiza el registro existente en lugar de insertar uno nuevo:

```java
Optional<CommunityMember> existingMemberOpt = memberRepository.findByCommunityIdAndUserId(communityId, currentUserId);

if (existingMemberOpt.isPresent()) {
    CommunityMember existingMember = existingMemberOpt.get();
    if (existingMember.getStatus() == MemberStatus.ACTIVO) {
        throw new BusinessRuleException("Ya eres miembro activo de esta comunidad");
    }
    existingMember.setStatus(MemberStatus.ACTIVO);
    existingMember.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return communityMapper.toMemberResponseDto(memberRepository.save(existingMember));
}
```

---

### 4.2. `CommunityActivityServiceImpl`

#### Validación de Coherencia Cronológica (CU-49 / CU-50)
Valida que la fecha de cierre de un evento sea posterior a su fecha de inicio:

```java
if (request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
    throw new BusinessRuleException("La fecha de finalización no puede ser anterior a la fecha de inicio");
}
```

#### Restricción de Confirmación de Asistencia (CU-53)
Verifica que únicamente los miembros activos de la comunidad organizadora puedan inscribirse en sus actividades:

```java
boolean isMember = memberRepository.existsByCommunityIdAndUserIdAndStatus(
        activity.getCommunity().getId(), currentUserId, MemberStatus.ACTIVO);
if (!isMember) {
    throw new BusinessRuleException("Debes ser miembro de la comunidad para confirmar asistencia a sus actividades");
}
```

---

## 5. Manejo Transaccional y Excepciones de Dominio

La capa de servicios no maneja bloques `try-catch` para excepciones de negocio; en su lugar, lanza excepciones puras de dominio que son interceptadas centralizadamente por `@RestControllerAdvice`:

```
                 ┌────────────────────────────────────────┐
                 │       Capa de Servicios (ServiceImpl)   │
                 └──────────────────┬─────────────────────┘
                                    │ lanza
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│ResourceNotFoundExcept│ │BusinessRuleException │ │ForbiddenActionExcept │
└───────────┬──────────┘ └──────────┬───────────┘ └──────────┬───────────┘
            │                       │                       │
            └───────────────────────┼───────────────────────┘
                                    │ capturado por
                                    ▼
                     ┌─────────────────────────────┐
                     │    GlobalExceptionHandler   │
                     └──────────────┬──────────────┘
                                    │ transforma a
                                    ▼
                     ┌─────────────────────────────┐
                     │ HTTP Status Response (JSON) │
                     └─────────────────────────────┘
```

* **`ResourceNotFoundException`:** Lanza HTTP `404 Not Found` cuando una entidad buscada por ID y estado activo no existe.
* **`BusinessRuleException`:** Lanza HTTP `409 Conflict` o `400 Bad Request` cuando se viola una validación de dominio (ej. nombres duplicados, fechas inconsistentes).
* **`ForbiddenActionException`:** Lanza HTTP `403 Forbidden` cuando el usuario no cumple con el rol de `ADMINISTRADOR`.