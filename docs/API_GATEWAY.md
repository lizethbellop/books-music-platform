# Integración del frontend con el Gateway

`frontend/lib/core/config/api_config.dart` centraliza las URLs del backend.
GATEWAY_URL vale http://localhost:8084 por defecto. Debe ser un origen sin
barra final ni rutas. Los antiguos defines AUTH_API_URL, BOOKS_API_URL,
MUSIC_API_URL y PROFILE_API_URL ya no configuran el frontend.

```bash
cd frontend
flutter run -d chrome --web-port 3000 --dart-define=GATEWAY_URL=http://localhost:8084
```

En emulador Android usar http://10.0.2.2:8084; en equipo físico, la dirección
LAN del servidor. Reiniciar/recompilar al cambiar el define.

## Routing y predicates

Un predicate es una condición para seleccionar una ruta. Al coincidir el path,
el Gateway reenvía la petición al servicio correspondiente:

| Predicate | Destino local |
| --- | --- |
| `/api/v1/auth/**` | `http://localhost:8083` |
| `/api/v1/users/**` | `http://localhost:8083` |
| `/books/**` | `http://localhost:8081` |
| `/api/music/**` | `http://localhost:8080` |
| `/api/profiles/**` | `http://localhost:8082` |

La ruta de usuarios es necesaria para mostrar nombres públicos en las reseñas.
Conservar el path completo, sin StripPrefix ni RewritePath, consultas, cuerpos
y encabezado Authorization. Configurar CORS para el origen Flutter web,
incluidos OPTIONS y Authorization. Si se usa Docker, los destinos deben ser
accesibles desde el contenedor; localhost se refiere al propio contenedor.

**Estado encontrado:** no existía la rama remota feature/api-gateway y main
no incluía código del Gateway. Esta integración del frontend requiere iniciar
el Gateway desarrollado fuera de esta copia o incorporar su código. No se
ha comprobado aquí una respuesta real a través del puerto 8084.

## Para explicar a compañeras y maestro

Antes Flutter conocía cuatro puertos; ahora conoce un único punto de entrada.
Los microservicios conservan sus responsabilidades y contratos. El Gateway
reenvía cada solicitud según su ruta y devuelve la respuesta del servicio.
Esto facilita cambiar el host en un solo lugar. Añade un salto de red y debe
estar disponible para que el frontend acceda al backend.

El servicio de usuarios sigue emitiendo los tokens. AuthSessionManager mantiene
el access token y lo renueva con refresh-token cuando corresponde. Libros y
música usan AuthenticatedApiClient; perfil obtiene el token mediante el mismo
gestor, también al subir fotos. Las llamadas protegidas envían
`Authorization: Bearer <accessToken>`. El Gateway debe reenviarlo y cada servicio
sigue validando JWT y permisos. Cambiar URLs no implementa validación JWT en el
Gateway ni sustituye la seguridad de los servicios. UUID, JSON, métodos,
códigos HTTP y multipart se conservan.

## Comprobaciones

```bash
cd frontend
flutter analyze
flutter test
flutter test --dart-define=GATEWAY_URL=http://127.0.0.1:8084 test/api_config_test.dart test/authenticated_catalog_api_test.dart test/review_usernames_test.dart
```

Las pruebas con clientes simulados comprueban configuración, destino de las
peticiones, rutas, Bearer y contratos existentes. No prueban conectividad real.
Con servicios y Gateway activos, comprobar login, renovación, búsqueda,
favoritos, reseñas con nombres, perfil, foto, preferencias, listas y logout.
Verificar en Network que el backend se consulte por 8084, y comprobar rechazo
de tokens inválidos y CORS desde Flutter web.

## Observabilidad: fase posterior

Agregar logs de routing con identificador de correlación, métricas de latencia,
volumen y errores por servicio, health checks y trazas distribuidas. Permiten
identificar dónde falla o tarda una solicitud. No registrar contraseñas,
Authorization ni refresh tokens. Esta fase no está implementada por el cambio
de URLs.
