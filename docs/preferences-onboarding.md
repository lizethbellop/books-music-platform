# Preferencias al terminar el registro

## Flujo de la persona

1. Crea su cuenta con el formulario existente.
2. La app inicia sesión con las credenciales que acaba de introducir. No las guarda
   ni activa Recordarme automáticamente.
3. Aparece “¿Qué te gusta?” con buscador y filtros Libros, Canciones y Artistas.
4. Puede seleccionar varios resultados de distintas búsquedas, quitar selecciones
   y elegir hasta 20 elementos en total. No se exige un mínimo ni ambos tipos.
5. “Guardar y continuar” agrega toda la selección a las preferencias de Mi perfil
   y lleva a Inicio. “Omitir por ahora” continúa sin enviar un lote de preferencias.

Si la cuenta se creó pero el login automático falla, la app va a Iniciar sesión
con la intención de abrir preferencias después. No vuelve a registrar la cuenta.
Si cierra completamente la app antes de ese login, puede gestionar sus preferencias
desde Mi perfil; no se almacena una contraseña ni una sesión forzada para retomar.
Las cuentas que ya iniciaban sesión normalmente siguen entrando en Home.

La pantalla usa OpenLibrary a través de books-service y Spotify a través de
music-service, con el Gateway y el Bearer actual de musa. No inventa catálogos,
géneros, portadas ni artistas. Los álbumes no aparecen como canciones o artistas.

Las selecciones se mantienen al cambiar de filtro o consulta. El buscador espera
400 ms tras escribir y también admite Enter/botón; descarta respuestas antiguas.
Hay estados de carga, falta de resultados, error y reintento. Durante el guardado
se bloquean cambios y navegación hacia atrás para no enviar lotes diferentes.
Los botones de continuar/omitir se mantienen en el pie de la pantalla.

## Cómo se guardan

Antes de guardar se asegura el perfil propio con el POST idempotente existente
`/api/profiles/me`. Después:

```http
POST /api/profiles/me/preferences/batch
Authorization: Bearer <token de musa.>
Content-Type: application/json

{"elements":[
  {"elementType":"BOOK","referenceId":"OL5735363W"},
  {"elementType":"SONG","referenceId":"<Spotify track ID>"},
  {"elementType":"ARTIST","referenceId":"<Spotify artist ID>"}
]}
```

El ejemplo musical indica formato: la app envía los IDs reales de los resultados.
El controlador obtiene el usuario del JWT; no recibe userId del cliente. Reutiliza
las tablas `preferences` y `preference_elements`: no hay migraciones nuevas.

El lote acepta 1..20 elementos válidos, rechaza entradas nulas y exige Work IDs
OL...W para libros. Agrega sin borrar preferencias previas y deduplica por tipo/ID.
El guardado es transaccional: se confirma completo o se revierte. Bloquea la fila
del perfil durante el lote para serializar reintentos simultáneos del onboarding.
Las consultas de catálogos no se hacen dentro de esta transacción.

Si se pierde la respuesta después de confirmar, reenviar el lote no duplica los
mismos elementos. La pantalla conserva su selección ante un error para reintentar.
“Omitir” no revierte algo que el servidor ya hubiera confirmado antes de perderse
la respuesta; por eso el mensaje distingue no poder confirmar el guardado.
El endpoint no fabrica metadatos ni genera calificaciones: guarda las referencias,
y Mi perfil resuelve su contenido con las APIs existentes al consultarlas.

## Cómo se usa al llegar a Inicio

El endpoint de semillas de libros combina las referencias BOOK de **preferencias
más listas personales** del perfil autenticado, deduplicadas. Así una cuenta nueva
puede recibir sugerencias a partir de sus libros preferidos sin crear una lista.
También excluye esas obras de los candidatos para no recomendar lo que ya eligió.

No mezcla canciones/artistas con los libros. La música queda guardada y visible
en preferencias para la integración musical futura; su recomendador no se modificó.
Si sólo elige música o no elige nada, Home sigue mostrando descubrimiento de libros.
No se incorporaron listas de amigos ni se modificaron sus reglas de privacidad.

## Archivos principales

- `frontend/lib/features/onboarding/`: pantalla y cliente de preferencias.
- `frontend/lib/features/auth/presentation/screens/register_screen.dart`: registro
  seguido de login y onboarding, con recuperación si el login falla.
- `frontend/lib/main.dart`, `app_routes.dart`, `login_screen.dart`: navegación y
  protección de la pantalla mediante la sesión actual.
- `profile-service/PreferenceController`, `PreferenceService` y
  `SaveInitialPreferencesRequest`: guardado por lote.
- `PreferenceElementRepository` y `UserListService`: semillas de libros combinadas.

## Ejecutarlo y verificarlo

Recompilar/reiniciar profile-service y volver a ejecutar Flutter. Users, books,
music, Gateway y el recomendador deben estar disponibles con sus configuraciones
habituales. No hace falta reentrenar ni reemplazar el artefacto de recomendaciones:
Home vuelve a consultar las referencias privadas al cargar o actualizar.

Desde frontend: `flutter test --no-pub` y analizar los archivos de onboarding/auth.
Desde backend/profile-service: `./mvnw test`.

Los tests de widget usan catálogos HTTP de prueba. Las pruebas H2 ejecutan el
almacenamiento transaccional, reintentos, rollback y aislamiento reales de repositorios.
No crean cuentas reales ni envían correos. La comprobación con todos los servicios,
PostgreSQL y las credenciales de Spotify en funcionamiento queda como prueba local
end-to-end; no se afirma que los mocks sustituyan esa comprobación.
