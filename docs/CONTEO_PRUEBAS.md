# Inventario de pruebas — 8 de octubre de 2026

| Servicio | Unitarias sin contexto Spring | Controladores/contexto Spring | BD/Redis reales | Total backend |
|---|---:|---:|---:|---:|
| Usuarios/autenticación | 42 | 9 | 9 | 60 |
| Perfil | 34 | 19 | 3 | 56 |
| Libros | 7 | 8 | 2 | 17 |
| Música | 12 | 10 | 3 | 25 |
| **Total** | **95** | **46** | **17** | **158** |

Se cuentan los métodos @Test de los cuatro servicios. Las pruebas unitarias usan objetos, Mockito o transporte HTTP simulado sin arrancar un contexto Spring. Las pruebas MockMvc/WebMvcTest y los contextos Spring/H2 se cuentan en otra columna para no llamarlas unitarias puras. La última columna de integración identifica PostgreSQL/Redis reales y pruebas de arranque dependientes de ellos.

Hay además **38 pruebas Flutter** ejecutadas, entre modelos, HTTP simulado, widgets y navegación. Incluyen tres casos generados por un bucle para anchos distintos; por eso no basta contar las líneas testWidgets. Backend más Flutter: **196 casos**. El inventario describe pruebas existentes, no que todas se ejecuten siempre: algunas integraciones requieren variables MUSA_*_TEST y pueden omitirse.

Además hay **4 pruebas del script de configuración local** en scripts/test_setup_local_env.py. Total del inventario incluyendo esas pruebas: **200 casos**. La revisión local del 8 de octubre completó los **200 casos de la versión actual**: 158 backend, 38 Flutter y 4 del script. Se verificaron por grupos; las últimas 3 integraciones de repositorios/arranque de Usuarios se ejecutaron en una BD temporal aislada, eliminada al terminar. Esto no reemplaza la prueba manual del correo SMTP ni la ejecución en las computadoras de las compañeras.
