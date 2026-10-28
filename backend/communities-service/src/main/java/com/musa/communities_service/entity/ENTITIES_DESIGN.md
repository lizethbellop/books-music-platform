# Guía de Diseño y Arquitectura JPA — `communities-service`

Este documento detalla las decisiones técnicas, patrones de diseño y mapeo relacional-objeto (ORM) aplicados en las entidades del microservicio `communities-service`.

---

## 1. Justificación de Diseño: Java Puro (Jakarta JPA) vs. Lombok en Entidades

Se ha optado por implementar las entidades utilizando **Java Puro y la especificación Jakarta Persistence API**, evitando el uso de Lombok en el modelo de dominio por las siguientes razones:

1. **Encapsulamiento e Inmutabilidad Controlada:** Lombok genera `setters` indiscriminados. Atributos críticos de auditoría e identidad (`id`, `createdAt`, `createdBy`, `userId`) no deben ser modificados por código externo una vez asignados.
2. **Evitar Errores Runtime en Hibernate:**
    * Anotaciones de Lombok como `@Data` o `@EqualsAndHashCode` calculan el `hashCode` usando todos los campos. Al mutar una entidad o asignársele un ID persistido, su hash cambia, rompiendo colecciones de Java (`Set<Entity>`).
    * `@ToString` automático provoca fallos de desbordamiento de pila (`StackOverflowError`) al intentar imprimir relaciones bidireccionales `@ManyToOne` / `@OneToMany`.
3. **Constructores Protegidos de Dominio:** La especificación JPA requiere un constructor sin argumentos para instanciar la entidad mediante *reflection*. Al definirlo manualmente como `protected`, se cumple la regla del framework pero se impide que la capa de aplicación cree entidades "vacías" o incoherentes.
4. **Independencia de Plugins y Compiladores:** Elimina problemas de sincronización, firmas de bytecode o incompatibilidades en el entorno de desarrollo (IDE) y en el proceso de compilación del motor Maven.

---

## 2. Diccionario de Anotaciones Jakarta JPA Utilizadas

| Anotación | Propósito y Funcionamiento en la Base de Datos |
| :--- | :--- |
| `@Entity` | Marca la clase de Java como una entidad persistente gestionada por el contexto de persistencia de JPA (EntityManager). |
| `@Table(name = "...")` | Mapea la clase con el nombre exacto de la tabla física en PostgreSQL (`communities`, `community_members`, etc.). |
| `uniqueConstraints` | Define restricciones de unicidad a nivel de tabla SQL. Ejemplo: `@UniqueConstraint(name = "uq_community_member", columnNames = {"community_id", "user_id"})`. Garantiza en base de datos que un usuario no se registre dos veces en el mismo grupo. |
| `@Id` y `@GeneratedValue` | Especifica la Clave Primaria (PK). La estrategia `GenerationType.UUID` le delega la generación automática del identificador único universal de 128 bits a Hibernate. |
| `@Column(updatable = false)` | Bloquea las operaciones SQL `UPDATE` sobre columnas específicas (`createdAt`, `createdBy`, `userId`). Protege la integridad de los datos históricos. |
| `@Enumerated(EnumType.STRING)` | Mapea enumeraciones de Java como texto explícito (`VARCHAR`) en SQL en lugar de números ordinales (`0, 1, 2`). Si el orden de los enums cambia en el código, los datos en la base de datos no se corrompen. |
| `@ManyToOne(fetch = FetchType.LAZY)` | Establece la relación Clave Foránea (FK). Se configura en modo **LAZY** (carga perezosa) para evitar consultas pesadas innecesarias (`N+1 Problem`); la entidad relacionada solo se trae de la base de datos si se invoca explícitamente su getter. |
| `@JoinColumn(name = "...")` | Declara la columna física de la base de datos que actúa como Clave Foránea (`community_id`, `activity_id`). |
| `@PrePersist` y `@PreUpdate` | Métodos del ciclo de vida de JPA (*Lifecycle Callbacks*). Se ejecutan automáticamente antes de las sentencias SQL `INSERT` y `UPDATE` respectivamente. Garantizan que `createdAt` y `updatedAt` utilicen marcas de tiempo con zona horaria UTC (`OffsetDateTime.now(ZoneOffset.UTC)`). |

---

## 3. Convención de Documentación en el Proyecto

* **Entidades JPA:** Se mantienen limpias sin bloques de Javadoc para no recargar el código fuente con información redundante expuesta en las anotaciones JPA y en las variables de campo.
* **Capa de Repositorios y Servicios:** A partir de la interfaz de repositorio (`Repository`) en adelante, **es obligatorio el uso de Javadocs** en clases y métodos para documentar las firmas de consulta, parámetros y comportamientos esperados.