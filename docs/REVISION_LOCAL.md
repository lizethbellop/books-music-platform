# Revisión local — 8 de octubre de 2026

## Correcciones

- Música: comas faltantes en seed.sql. Ejecutado dos veces en un esquema temporal: inserta 21 registros y no duplica. Esos registros tienen IDs de ejemplo, no IDs reales de Spotify.
- Instalación: setup-local-env.py ahora comparte una sola JWT_SECRET entre los cuatro servicios. Reutiliza la existente, completa claves vacías y conserva las demás credenciales. Si detecta claves distintas o inválidas, se detiene antes de modificar archivos. Sus pruebas usan carpetas temporales; no se ejecutó sobre los .env reales de Liz.
- Usuarios: un JWT malformado, una cuenta inexistente/desactivada o un token inválido ahora recibe 401 con mensaje JSON, sin convertirse en un error interno ni autenticar una cuenta desactivada. El flujo válido sigue funcionando.
- Flutter: las respuestas HTTP vacías o HTML conservan su código de error. Esto permite reconocer un 401 de renovación y cerrar la sesión local. Los errores de validación de campos muestran el mensaje del backend. Las solicitudes de autenticación tienen un límite de espera de 15 segundos.
- Documentación: inventario y estado del contrato de autenticación actualizados.

## Verificaciones realizadas

| Componente | Pruebas ejecutadas sin fallos |
|---|---:|
| Usuarios: unitarias/controladores | 46 |
| Usuarios: username y Redis reales | 5 |
| Perfil: incluye PostgreSQL y concurrencia | 56 |
| Libros: incluye PostgreSQL/HTTP | 17 |
| Música: incluye PostgreSQL/HTTP | 25 |
| Flutter | 35 |
| Script de configuración | 4 |
| Total ejecutado | 188 |

flutter analyze no encontró problemas. Los cinco archivos OpenAPI se pudieron leer como YAML y sus referencias internas existen; esto no equivale a comprobar todos los endpoints contra cada detalle de los contratos. git diff --check pasó. Los cuatro .env están ignorados y no se encontraron sus secretos en los archivos candidatos a Git.

## Alcance y comprobación manual pendiente

Se revisó la copia local de los cuatro servicios activos, su configuración, integración, scripts y Flutter. No es una garantía de ausencia de cualquier error en todos los servicios futuros del proyecto.

No se ejecutaron 3 integraciones adicionales de Usuarios (repositorios/arranque general que requieren preparación distinta); sí se ejecutaron las pruebas dirigidas de registro, login, renovación y Redis. El inventario distingue casos existentes y ejecutados.

Queda repetir el recorrido manual de la guía con las credenciales externas de cada integrante. No se ejecutaron estos cambios en Windows, Android o iOS. No se iniciaron backend permanentes ni se publicó nada en GitHub. Los cambios de esta revisión no requieren una migración nueva de BD.

## Integración posterior de recuperación de Derly

Se integró el flujo olvidé mi contraseña de feature/users-login sin sustituir la gestión de sesión actual. En esta verificación pasaron 51 pruebas unitarias/controladores de Usuarios, 6 de integración username/recuperación/Redis y 38 Flutter; flutter analyze sin problemas. Perfil, Libros y Música no se modificaron en esta integración y conservan las pruebas ya verificadas arriba. El inventario actual está actualizado en CONTEO_PRUEBAS.md. Para el correo real y recorrido manual consultar RECUPERACION_CONTRASENA.md.

## Cierre de la revisión

Se ejecutaron sin errores las 3 integraciones de Usuarios que estaban pendientes, utilizando una BD PostgreSQL temporal aislada y eliminada después. Quedan comprobados los 200 casos del inventario de la versión actual (158 backend, 38 Flutter y 4 del script). La revisión anterior documentaba esas pruebas como pendientes; este cierre actualiza ese estado. Se volvieron a comprobar los cinco contratos YAML y sus referencias internas, git diff --check y la ausencia de secretos locales en archivos candidatos a Git. No se encontraron errores adicionales en estas comprobaciones. El correo SMTP real y las otras plataformas siguen requiriendo prueba manual.
