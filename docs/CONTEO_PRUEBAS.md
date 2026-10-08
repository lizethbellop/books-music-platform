# Inventario de pruebas — 8 de octubre de 2026

| Servicio | Unitarias sin contexto Spring | Controladores/contexto Spring | BD/Redis reales | Total backend |
|---|---:|---:|---:|---:|
| Usuarios/autenticación | 33 | 7 | 8 | 48 |
| Perfil | 34 | 19 | 3 | 56 |
| Libros | 7 | 8 | 2 | 17 |
| Música | 12 | 10 | 3 | 25 |
| **Total** | **86** | **44** | **16** | **146** |

Se cuentan los métodos @Test de los cuatro servicios. Las pruebas unitarias usan objetos, Mockito o transporte HTTP simulado sin arrancar un contexto Spring. Las pruebas MockMvc/WebMvcTest y los contextos Spring/H2 se cuentan en otra columna para no llamarlas unitarias puras. La última columna de integración identifica PostgreSQL/Redis reales y pruebas de arranque dependientes de ellos.

Hay además **31 pruebas Flutter** ejecutadas, entre modelos, HTTP simulado, widgets y navegación. Incluyen tres casos generados por un bucle para anchos distintos; por eso no basta contar las líneas testWidgets. Backend más Flutter: **177 casos**. El inventario describe pruebas existentes, no que todas se ejecuten siempre: algunas integraciones requieren variables MUSA_*_TEST y pueden omitirse.
