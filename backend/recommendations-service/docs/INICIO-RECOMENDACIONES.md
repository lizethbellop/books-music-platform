# Catálogo inicial y Home

## Catálogo público y crecimiento automático

La carga original incluía 12 libros y 25 referencias musicales. El catálogo ya
crece automáticamente según la actividad positiva. seed_catalog.sql se regeneró
con 55 elementos públicos preparados al verificar este cambio; no representa una
cobertura completa ni un límite fijo de tamaño.

Fuentes de la preparación inicial de libros: examples/book_catalog_sources.json.
Correspondencias musicales iniciales: examples/music_catalog_verified.csv.
Los elementos automáticos conservan sus IDs/URL de fuente en PostgreSQL.
Tres referencias musicales no se incluyeron por duplicar una obra o no tener una
correspondencia única; ver examples/music_catalog_verified.pending.json.
No se sustituyeron por coincidencias aproximadas.

## Compartir con el equipo

1. Crear recomendaciones con database/setup.sql si no existe. Contiene usuario,
   base, tablas de características, cola y presupuesto. Completar los marcadores solo localmente.
2. Para bases existentes aplicar las migraciones numeradas necesarias, incluidas
   database/004_automatic_book_catalog.sql, database/005_provider_budgets.sql
   y database/006_automatic_music_catalog.sql. No recrear la base ni el usuario.
3. Ejecutar database/seed_catalog.sql dentro de recommendations_db.
4. En Profile aplicar database/migrations/001_album_elements.sql, si la base ya existía.
5. Completar .env según .env.example. Nunca subir claves ni contraseñas.

El seed contiene solo IDs, características y metadatos públicos. No contiene
usuarios, listas, reseñas, tokens ni claves. Se comprobó ejecutándolo dos veces
sin duplicar filas. No hace llamadas a proveedores: las compañeras pueden cargar
los mismos datos sin consumir tokens.
MODEL_PATH es opcional para este motor; dejarlo vacío si no se dispone del
artefacto Goodbooks anterior. GROQ_MODEL debe coincidir con la versión del seed
(openai/gpt-oss-20b). La clave Groq solo es necesaria para nuevas extracciones.

## Interfaz incorporada

Home utiliza /books/knn/me y /music/{artists,albums,songs}/me a través del gateway.
Muestra carruseles con carga, error, reintento, falta de resultados y descubrimiento.
Los títulos distinguen sugerencias personalizadas de descubrimiento sin forzar
una temática que el resultado no respalde.
Las tarjetas abren BookDetailPage o MusicDetailPage con el usuario de la sesión;
el carrusel se vuelve a consultar con Actualizar. No se envían userIds para elegir gustos ajenos.
Las preferencias iniciales, listas y preferencias del perfil admiten álbumes.

Las imágenes usan los datos disponibles. Para álbumes puede consultarse
Cover Art Archive a partir del release-group verificado. Si no hay imagen o falla,
se presenta un marcador visual; no se inventan fotos ni portadas.
La vista previa de pruebas usa catálogo real y gustos de ejemplo, no una sesión
real de la usuaria, y sustituye imágenes de red por marcadores.

## Comprobación manual pendiente

Reiniciar Books, Music y Profile para cargar las nuevas rutas, además del gateway
y el recomendador. Users y Redis son necesarios para iniciar sesión. PostgreSQL
debe estar activo. No hay que desbloquear cuentas ni generar tokens manualmente
para la app: usar el login normal con una cuenta válida.

Abrir Flutter y comprobar Home, apertura de detalles, regreso y actualización manual,
selección de un álbum y cambios de gustos tras guardar o calificar.
Si falta preparar un gusto, la API informa perfil parcial y encola su preparación;
no inventa resultados ni hace llamadas masivas al abrir Home.

## Preparar más datos

Los importadores y sus fuentes están documentados en CATALOGO-KNN.md y MUSICA-KNN.md.
resolve_music_catalog admite un CSV de IDs y verificación directa por URL/ISRC;
rechaza casos ambiguos. Las consultas MusicBrainz respetan una pausa y tienen
reintentos limitados para fallos temporales. La caché de fuentes está en artifacts/,
fuera de Git. No se envía contenido de Spotify al modelo de lenguaje.
export_catalog vuelve a generar seed_catalog.sql a partir del catálogo público.

## Validación

- Python: 111 pruebas aprobadas, incluidas 20 optativas de cola/presupuesto en PostgreSQL aislado.
- Flutter: 92 pruebas aprobadas; análisis sin incidencias.
- Se comprobó visualmente la disposición de los cuatro carruseles con catálogo real.
- SQL del catálogo validado en un esquema de PostgreSQL revertido al terminar.
- La sesión real y la pertinencia de cada recomendación todavía necesitan revisión.

## Libros similares en el detalle

GET /api/recommendations/books/{external_id}/similar?limit=8 requiere la sesión
normal y acepta de 1 a 20 resultados. Reutiliza el mismo KNN con el libro abierto
como única referencia, lo excluye de candidatos y no consulta gustos personales.
La pantalla de detalle incluye «Más libros como este» debajo de las reseñas;
cada tarjeta abre el detalle correspondiente conservando la sesión.
El ranking usa solo características guardadas. Si falta preparar un libro,
se conserva reference_not_prepared y se añade preparation.status=pending.
La solicitud encola su preparación y la búsqueda de candidatos relacionados;
un trabajador en segundo plano consulta fuentes y Groq cuando hace falta.
El detalle consulta una vez al abrirse y muestra los similares disponibles, sin
botón de refresh ni temporizadores. Solo Home permite Actualizar.
No se bloquea el detalle esperando al proveedor ni se inventan resultados.
Reiniciar recomendaciones y Flutter para comprobarlo con Normal People.

La política book-features-knn-v2 unifica guiones Unicode y espacios al comparar
etiquetas, y elimina el sufijo redundante «fiction» dentro de géneros compuestos
(por ejemplo, «romance fiction» y «romance»). No reescribe las características
guardadas ni requiere volver a extraerlas.

## Preparación automática de libros

- Home encola los libros usados como señales positivas. El detalle encola el libro abierto.
- Los trabajos son públicos y compartidos en book_catalog_jobs; no contienen userId ni tokens.
- El trabajador arranca con el servicio y procesa una tarea a la vez, incluso con varios procesos.
- Para un libro nuevo intenta reutilizar Books mediante un token mantenido solo en memoria
  hasta 120 segundos. Si no está disponible o el proceso reinició, utiliza OpenLibrary pública.
- Guarda en la cola una copia de los metadatos públicos para reutilizarla si debe reintentar.
- Las características vigentes de book_features se reutilizan sin llamar a Groq.
- Tras preparar una referencia busca hasta 3 candidatos nuevos por etiquetas en OpenLibrary.
  No son recomendaciones todavía: se preparan, se guardan y KNN decide sus similitudes.
  Los candidatos no generan nuevas búsquedas en cadena. Una referencia se expande una vez
  por versión/modelo; no se pretende descargar todo OpenLibrary ni refrescarlo continuamente.
- La cola admite hasta 200 trabajos activos. Los límites alcanzados se comunican como
  pendientes/ocupado; no se eliminan datos ni se hacen llamadas masivas.
- book_provider_budget limita Groq a 40 intentos por día UTC y separa llamadas 35 segundos.
  Cuenta también las extracciones manuales; las lecturas de caché no gastan presupuesto.
  El presupuesto es compartido por esta base, no un cupo nuevo por cada usuario.
- Las respuestas 429 se posponen; el presupuesto diario se reanuda al día siguiente.
  Los 429 también tienen un máximo de 4 intentos por trabajo; los aplazamientos
  debidos al presupuesto local no realizan solicitudes HTTP ni gastan intentos.
  Otros errores se reintentan hasta 4 intentos y quedan como fallidos si persisten.
  Volver a consultar una referencia fallida después de 24 horas permite encolarla otra vez.
- Un trabajo interrumpido se recupera tras una concesión de 5 minutos. No se pierden
  características ya guardadas. Sin clave válida de Groq no se pueden preparar libros nuevos.
- La interfaz conserva los resultados existentes. Consulta al abrir; solo Home
  permite Actualizar. No hay consultas periódicas ni recarga al volver de un detalle.
- Música también amplía su catálogo automáticamente; consultar MUSICA-KNN.md.

Configuración opcional en .env (valores predeterminados seguros):

```dotenv
BOOK_CATALOG_AUTOPREPARE=true
BOOK_CATALOG_GROQ_DAILY_LIMIT=40
BOOK_CATALOG_GROQ_INTERVAL_SECONDS=35
BOOK_CATALOG_CANDIDATES_PER_BOOK=3
```

Instalación nueva: setup.sql ya incluye las tablas nuevas. Instalación existente:
ejecutar 004_automatic_book_catalog.sql conectado a recommendations_db como propietario.
La migración ya se aplicó en la computadora local, sin tocar los datos previos.

Pruebas sin proveedores reales:

```bash
RUN_CATALOG_DB_TESTS=1 .venv-service/bin/python -m pytest -q
```

Las pruebas optativas de PostgreSQL crean un esquema aislado y lo eliminan al terminar.
Validan DDL nuevo y migración repetible, recuperación, presupuesto, concurrencia y caché.
No envían solicitudes a Groq, OpenLibrary ni Books.

## Presupuestos por proveedor

Los siguientes son límites propios configurables, no cuotas garantizadas por los proveedores.
Cada cupo es compartido entre usuarios/procesos que utilicen la misma base de datos.
Las instalaciones con bases independientes tienen sus propios contadores.
Los contadores sobreviven a reinicios y se renuevan por día UTC.

| Proveedor | Preparación/importadores de Recomendaciones | Navegación | Total diario predeterminado |
| --- | --- | --- | --- |
| Groq | 40, intervalo mínimo 35 segundos | No se utiliza | 40 |
| OpenLibrary | 150, intervalo mínimo 3 segundos | Books: 200; máximo 20/minuto | 350 |
| Spotify | 20 para verificación de IDs automática/manual, intervalo 2 segundos | Music: 180; máximo 30/minuto | 200 |
| MusicBrainz | 150, intervalo mínimo 2 segundos | No se utiliza directamente | 150 |

Son asignaciones independientes por servicio, no un único contador entre tres bases.
El catálogo inicial vacío no puede consumir el cupo reservado para navegación.
Spotify incluye las solicitudes de token: se reutiliza el token vigente en cada proceso.
Los intentos fallidos y redirecciones Python también consumen cupo; leer caché no consume.
No hay reintento automático de Spotify. Un 429 activa una pausa compartida en el servicio.
Los 429 de preparación de libros tienen como máximo 4 intentos, además del presupuesto.

Cuando se agota un cupo, las consultas guardadas siguen disponibles. Las nuevas
consultas de Books/Music responden 503 y Retry-After; los trabajos de preparación
se aplazan sin efectuar llamadas externas. Si falta la tabla/registro, se bloquea
la llamada externa en lugar de continuar sin control. Los importadores también
usan los cupos; una pausa de presupuesto puede requerir ejecutarlos nuevamente.

Migraciones nuevas (ya aplicadas localmente):
- recommendations-service/database/005_provider_budgets.sql
- books-service/database/migrations/001_provider_budget.sql
- music-service/database/migrations/002_provider_budget.sql
Los respectivos setup.sql/schema.sql incluyen estas tablas para instalaciones nuevas.
Ningún script reinicia contadores existentes ni almacena claves o tokens.

Configuración por servicio en su propio .env:
- Recomendaciones: OPENLIBRARY_DAILY_LIMIT=150, OPENLIBRARY_INTERVAL_SECONDS=3,
  SPOTIFY_DAILY_LIMIT=20, SPOTIFY_INTERVAL_SECONDS=2, además de BOOK_CATALOG_GROQ_*.
- Books: OPENLIBRARY_DAILY_LIMIT=200, OPENLIBRARY_MINUTE_LIMIT=20.
- Music: SPOTIFY_DAILY_LIMIT=180, SPOTIFY_MINUTE_LIMIT=30.
Para desactivar las llamadas de un proveedor, establecer su cupo diario en 0.
MusicBrainz: MUSICBRAINZ_DAILY_LIMIT=150, MUSICBRAINZ_INTERVAL_SECONDS=2.
No reutilizar un mismo archivo de variables entre estos servicios: tienen cupos distintos.
Las descargas de portadas/fotos en el navegador son independientes de estas APIs.
MusicBrainz tiene ahora presupuesto de 150 intentos diarios y 2 segundos entre llamadas;
participa en el crecimiento automático musical y comparte cupo con los importadores.
Reiniciar Books, Music y Recomendaciones para activar los cambios.

## Refinamiento musical y prueba de Lush Life

La ampliación automática musical se dispara al abrir los carruseles con señales
positivas; deja de depender de un catálogo exclusivamente manual. Reutiliza lo
guardado, amplía fuentes con límites y muestra avisos de preparación en la interfaz.
El cálculo reduce coincidencias generales y etiquetas de listas de éxitos, elimina
repeticiones y limita a dos resultados por artista sin rellenar con más del mismo.
Se verificó la incorporación automática de Zara Larsson, VENUS y canciones suyas
partiendo de Lush Life. Para que una cuenta tome Lush Life como señal positiva debe
calificarla con 4–5, marcarla como favorita o guardarla en lista/preferencias; una
reseña sin esas señales no implica que le guste. No se alteraron datos de cuentas.

La base local ya tiene la migración 006 aplicada. Reiniciar Recomendaciones y usar
R mayúscula en Flutter. El servicio inicia ambos trabajadores en segundo plano.
Los scripts setup.sql y seed_catalog.sql incluyen el esquema y datos públicos
actualizados; no incluyen trabajos pendientes, presupuestos usados ni información personal.

## Actualizar Home y mostrar similares en detalles

Solo Home muestra controles de actualización. Cada pulsación vuelve a consultar
las señales actuales y el catálogo, manteniendo las tarjetas previas mientras llega
la respuesta. El icono muestra progreso y se deshabilita durante esa solicitud.
Al terminar se muestra «Recomendaciones actualizadas» o «No hay nuevas sugerencias»
si el resultado no cambió. Actualizar no implica barajar resultados ni inventar
candidatos; si gustos y catálogo no cambiaron, el ranking puede ser idéntico.
Las pruebas usan dos respuestas distintas para comprobar que las tarjetas realmente
se sustituyen, y una tercera idéntica para comprobar la confirmación sin novedades.

Los detalles de libros, artistas, álbumes y canciones consultan similares al abrir
sin pedir actualizar. Si todavía no hay candidatos preparados, muestran disponibilidad
sin prometer que las APIs externas siempre responderán inmediatamente. La preparación
encolada sigue usando los presupuestos y la caché, y no se reinicia por cada refresh.

## Activar la conexión de las páginas y la corrección de portadas

La migración music-service/database/migrations/003_original_artwork.sql ya se
aplicó localmente sin borrar datos. Para otras instalaciones debe ejecutarse
conectado a music_db como propietario; el schema.sql nuevo también incluye el campo.
Reiniciar Books, Music y Recomendaciones, y hacer R mayúscula en Flutter.

Music ya muestra resultados del motor en Todo/Artistas/Álbumes/Canciones. Books
muestra recomendaciones personales en Todos, en lugar de exploración general.
Los favoritos explícitos de Books son señales positivas, siempre subordinadas a
la exclusión por notas menores de 4. Solo Home mantiene Actualizar.
La corrección de portadas se guarda en Music y no entra en los vectores del motor.
Se verificaron y corrigieron SOUR y 11 RAZONES usando la grabación y artistas exactos.
