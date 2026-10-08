# Reseñas y colecciones

Las reseñas siguen guardando el UUID del autor para la propiedad y los permisos. Flutter consulta GET /api/v1/users/usernames en Usuarios mediante Bearer para resolver usernames por lotes de hasta 100 UUID únicos. La respuesta solo contiene UUID y username de cuentas activas. No cambia las BD ni los endpoints de Libros y Música y no accede a otra BD desde esos servicios. El contrato está en contratos/rest/usuarios-publicos.yaml.

Si Usuarios está temporalmente inaccesible, se conservan las reseñas y se muestra Usuario; nunca se presenta el UUID como nombre. Para la cuenta activa también se utiliza el username de su sesión. AUTH_API_URL controla el origen de la consulta.

Las listas de Perfil muestran su nombre, descripción y fila horizontal de portadas, con título y autor/artista debajo. Abrir mantiene la búsqueda, agregado y eliminación de elementos. La vista previa se recarga al cerrar el diálogo. Preferencias utiliza la misma presentación con portadas verticales de 132 por 198 y BoxFit.contain para no recortar libros. Artistas se muestran en círculo; los elementos sin imagen usan un icono.

La navegación principal pertenece exclusivamente a AppShell/MusaNavigationShell. Música y Libros no añaden un sidebar propio. AppSidebar conserva un alias al único diseño claro para las llamadas anteriores. El avatar circular inferior se conserva.

Para que todo el equipo pruebe el mismo cambio, deben ejecutar la misma revisión del repositorio, configurar sus .env ignorados y aplicar la migración de username en su BD de Usuarios. Después, detener la app anterior y volver a iniciarla; si el navegador conserva el diseño antiguo, hacer una recarga completa. Cada computadora necesita sus credenciales y bases locales según CONFIGURACION_LOCAL.md.

Verificación manual antes de publicar: con dos cuentas distintas, crear reseñas de libro y canción y comprobar los nombres al entrar con la otra cuenta; revisar preferencias, listas y navegación de Música en móvil y escritorio. La migración de username está documentada en USERNAME_AUTENTICACION.md. No se hizo commit ni push.

Resultados de esta revisión: 31 pruebas Flutter, 40 pruebas de autenticación y la prueba de integración del directorio con PostgreSQL/Redis aprobadas. Flutter analyze sin incidencias. Compilaciones web y macOS debug correctas. Las pruebas de navegación verifican un único sidebar claro y las filas de portadas se probaron a 320, 390 y 1366 píxeles. Windows, Android e iOS requieren ejecución manual en sus equipos. Los backend quedan detenidos.
