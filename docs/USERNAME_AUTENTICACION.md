# Nombre de usuario

Registro requiere username, además de los campos anteriores. Usa de 3 a 40 letras ASCII, números o guiones bajos; se guarda en minúsculas y es único, incluso ante registros simultáneos. El login sigue siendo por correo y contraseña. roleName continúa definiendo el rol y sus permisos.

Login y refresh-token devuelven username. Flutter lo guarda con la sesión y lo muestra en el encabezado de Perfil. Las sesiones antiguas que no incluyen este campo se renuevan al restaurarlas.

En una instalación existente, aplicar backend/users-service/database/migrations/001_username.sql en la BD de autenticación antes de encender el servicio. La migración es idempotente y conserva los datos; las cuentas existentes reciben user_ seguido de su UUID sin guiones. Las cuentas nuevas eligen su username al registrarse. Cambiar el username después del registro queda fuera de este cambio.

La base local BDAuth ya se migró durante esta actualización. No se modificaron las bases de Perfil, Libros o Música. No subir los archivos .env ni el respaldo local.

## Verificación de este cambio

- 40 pruebas de autenticación y 24 de Flutter aprobadas; Flutter analyze sin incidencias.
- Prueba de integración con PostgreSQL local y Redis: registro con mayúsculas, persistencia normalizada, rechazo del duplicado, login, renovación conservando username/UUID/vencimiento y logout.
- El correo se simuló en las pruebas. Los registros de prueba se revirtieron y sus sesiones se revocaron. Los cuatro backend quedan detenidos.

Para probar manualmente, encender el entorno según PRUEBA_FLUJO_LOCAL.md, registrar una cuenta con un username nuevo (por ejemplo liz_bello), entrar y abrir Perfil. Su encabezado debe mostrar liz_bello.
