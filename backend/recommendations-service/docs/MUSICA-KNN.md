# Recomendaciones de artistas, álbumes y canciones

## Qué está implementado

Un KNN por características que comparte catálogo y señales entre tipos, pero
devuelve únicamente el tipo solicitado. No es un modelo entrenado con escuchas
de Spotify ni consulta automáticamente el historial personal de Spotify.

Endpoints autenticados del recomendador:
- `GET /api/recommendations/music/artists/me?limit=12`
- `GET /api/recommendations/music/albums/me?limit=12`
- `GET /api/recommendations/music/songs/me?limit=12`

El cliente envía su token normal de musa. El gateway existente reenvía estas rutas.
Los resultados personales usan `Cache-Control: no-store` y `Vary: Authorization`.
El ranking usa PostgreSQL y no llama a proveedores. La misma solicitud encola
preparación automática de semillas y candidatos; el trabajador de fondo sí consulta
MusicBrainz y, cuando hace falta verificar ISRC, Spotify. No utiliza Groq.

## Fuentes y características

Se obtienen géneros, etiquetas y créditos de artista de MusicBrainz. Se conservan
la URL de origen, el identificador MusicBrainz, una huella y la versión de características.
No se infieren letras, estados de ánimo ni instrumentos que la fuente no proporcione.
Los nombres y fechas devueltos por este motor proceden de MusicBrainz.
Los IDs de Spotify sirven para relacionar las acciones ya guardadas en musa y
abrir el elemento en el catálogo existente. Los IDs de Spotify no se vectorizan.
Las imágenes y otros detalles de presentación se pueden consultar con el flujo
de detalle existente después de obtener los resultados.

Spotify restringe la ingesta de su contenido en modelos de IA/ML:
https://developer.spotify.com/policy
Por eso no se envían sus descripciones, nombres, imágenes o audio a Groq ni se
utilizan como características del KNN.

MusicBrainz ofrece lectura de su API sin clave para uso no comercial, con User-Agent
identificable y como máximo una petición por segundo:
https://musicbrainz.org/doc/MusicBrainz_API
Mantener la atribución de la fuente y revisar su licencia al distribuir los datos:
https://musicbrainz.org/doc/About/Data_License

## Catálogo y correspondencias

Los IDs no son intercambiables. Un Spotify ID se relaciona con un MusicBrainz ID
solo después de verificar que representan al mismo artista, álbum o grabación.
No se acepta automáticamente la primera coincidencia por nombre.

Hay una plantilla sin datos inventados en `examples/music_mapping.example.csv`.
Archivo CSV esperado:

```csv
content_type,spotify_id,musicbrainz_id,verified
```

Agregar filas con valores reales y `verified=true` únicamente tras revisar la
correspondencia. Tipos: ARTIST, ALBUM, SONG. Para MusicBrainz usamos:
- ARTIST -> artist
- ALBUM -> release-group (la obra/álbum, no una edición concreta)
- SONG -> recording (la grabación, no una composición ni una edición)

Se impide mapear dos elementos del mismo tipo a la misma entidad MusicBrainz para
no duplicar álbumes por edición. Las ediciones especiales necesitan una política
posterior explícita, no forzar sus correspondencias.

Desde `backend/recommendations-service`:

```bash
.venv-service/bin/python -m recommender.prepare_music_catalog --mapping ruta/al/mapping.csv --max-items 10
```

No requiere login ni clave de Groq. Se ejecuta una sola importación a la vez.
Los elementos vigentes se reutilizan sin consultar la fuente. Para actualizar
etiquetas usar `--refresh`. El comando hace una pausa de 1.1 segundos antes de
cada petición y se detiene ante errores HTTP; lo guardado se conserva.

La importación manual requiere correspondencias verificadas. También existe
`resolve_music_catalog`: verifica enlaces explícitos de MusicBrainz a Spotify y,
para canciones sin enlace, un ISRC exacto con una sola grabación. No elige la primera
coincidencia por nombre. El catálogo inicial preparado puede cargarse desde
`database/seed_catalog.sql`, después del esquema y las migraciones.
MusicBrainz puede no tener etiquetas para determinados elementos; no se inventan.

## Gustos

Profile ofrece `GET /api/profiles/me/music-seeds`, con referencias de listas y
preferencias, deduplicadas por tipo e ID. Un perfil inexistente produce lista vacía.
Music ofrece `GET /api/music/recommendation-signals/me`, con notas, favoritos y
referencias de elementos reseñados, siempre limitados a la identidad del JWT.
No se envía el texto de reseñas al recomendador ni al proveedor.

Pesos iniciales:
- Listas y preferencias: 1.
- Favoritos explícitos: 1.5.
- Nota 4 a 5: de 2 a 3.
- Nota inferior a 4: elimina el elemento como semilla, incluso si está en favoritos/listas.

Las listas, preferencias, calificaciones, favoritos y elementos reseñados se
excluyen de resultados. Los elementos solamente reseñados sin nota/favorito/lista
no se convierten en gustos positivos.

Una semilla puede ayudar a encontrar otro tipo: por ejemplo, un artista puede
aportar géneros para encontrar canciones o álbumes, siempre que sus características
estén preparadas en el catálogo. `SONG:id` y `ALBUM:id` son identidades distintas.

## KNN

Distancia coseno sobre vectores de géneros (peso 3), etiquetas (2) e identidades
de artistas MusicBrainz (0.5). Esos pesos son de diseño inicial, no están optimizados.
Se usa la mejor afinidad ponderada con una semilla, y se diversifican los resultados
por los identificadores verificados de todos los artistas acreditados, con máximo
estricto de dos elementos por artista. No se rellena el espacio con más del mismo.
La política music-features-knn-v2 aplica además:
- Normalización de guiones y variantes como synthpop/synth-pop.
- Eliminación de etiquetas de listas de éxitos, semanas, años y códigos de masterizado.
- Una etiqueta de género no vuelve a contar como tag del mismo elemento.
- Reducción de etiquetas amplias (pop, rock, electronic, dance, music).
- IDF para reducir el peso relativo de características repetidas en el catálogo.
- Compartir solo una etiqueta amplia no basta para recomendar entre artistas distintos.
- Afinidad = 80% coseno ponderado + 20% por compartir artista MusicBrainz verificado,
  multiplicada por el peso relativo de la semilla. Es una decisión inicial de diseño.
- Supresión de títulos repetidos del mismo conjunto de artistas y explicaciones sin duplicados.
Compartir un artista sin otras etiquetas puede producir una recomendación, con
una explicación que lo dice expresamente. No se afirman semejanzas sonoras no medidas.

El índice se construye en memoria para cada consulta; esta implementación está
orientada al catálogo inicial pequeño. Un catálogo grande necesita reutilizar
índices, limitar semillas/trabajo y medir latencia antes del despliegue.

## Base de datos y configuración

Instalaciones nuevas: `database/setup.sql` crea usuario, base, book_features
(incluida metadata) y music_features. Completar los marcadores localmente;
no subir contraseñas. El bloque de creación de base se ejecuta sin transacción.

Instalaciones existentes: ejecutar `database/003_music_features.sql` en
recommendations_db. No recrear base ni usuario.

En Profile, `database/schema.sql` ya admite ALBUM. En bases existentes ejecutar
`backend/profile-service/database/migrations/001_album_elements.sql`.
Los DTO y el resolvedor de catálogo también aceptan álbumes. La interfaz de
Profile y el onboarding ya incluyen controles visuales para agregar álbumes.

Variables adicionales, sin secretos:
```
MUSIC_SERVICE_URL=http://localhost:8080
MUSICBRAINZ_USER_AGENT=musa-recommendations/0.1 (https://github.com/lizethbellop/books-music-platform)
```
El backend tiene defaults locales para ambas. La conexión usa RECOMMENDATIONS_DB_*
existente. No hacen falta otra base ni otra clave de IA.

## Respuestas y validación

Se distinguen `catalog_not_ready`, `profile_not_prepared`, `explore` y `knn_content`.
`missing_seed_keys` informa semillas sin preparar; `partial` indica perfil incompleto.
Los errores de los servicios no se convierten silenciosamente en gustos vacíos.

Las pruebas cubren identidad, tipado, prioridad de notas, exclusiones, cruce entre
tipos, cold start, procedencia y correspondencias. No sustituyen una sesión real
ni una evaluación de calidad con un catálogo verificado.

Home ya consume estas rutas en tres carruseles musicales y utiliza el KNN nuevo
para libros. Las tarjetas abren el detalle existente; Actualizar vuelve a consultar los resultados.
Listas, preferencias y selección inicial ya permiten álbumes.

## Resultado de comprobaciones locales

- Recomendador Python: 111 pruebas aprobadas (libros, música, cola y presupuestos).
- Music: 35 pruebas aprobadas.
- Profile: 66 pruebas aprobadas y 3 omitidas, sin fallos.
- La consulta de semillas musicales se probó con relaciones reales en H2,
  incluyendo deduplicación de álbumes y aislamiento entre perfiles.
- Las operaciones de almacenamiento musical y el DDL completo de setup.sql se
  comprobaron en PostgreSQL con un esquema de prueba revertido al terminar.
- La migración local de recomendaciones y la de Profile están aplicadas.

Después se preparó el catálogo real: 12 libros y 25 elementos musicales
(6 artistas, 8 álbumes y 11 canciones). Se usaron las APIs públicas y la cuota
configurada para preparar los libros. No se hicieron intentos de login.
Se conectó y comprobó la interfaz. Falta validar el recorrido con una sesión real.
No se ha hecho commit ni push.

## Ampliación automática musical

Al abrir cualquiera de los tres carruseles, las señales positivas del usuario
solicitan preparación y expansión. No hay que importar un CSV por cada gusto nuevo.
Las reseñas sin nota positiva/favorito/lista siguen siendo exclusiones, no gustos.
El texto de las reseñas no se analiza ni se envía a proveedores.

1. El servicio responde con las recomendaciones disponibles y preparation.status.
2. music_catalog_jobs guarda los trabajos públicos, compartidos entre usuarios.
3. Una referencia nueva se resuelve por enlace Spotify/MusicBrainz explícito o ISRC
   exacto y único. Se guardan características procedentes únicamente de MusicBrainz.
4. A partir de los créditos verificados, busca el artista, hasta dos álbumes y dos
   grabaciones, y un artista adicional por etiquetas con un álbum y una grabación.
   Cada semilla admite como máximo 8 candidatos, configurable hasta 12.
5. Las relaciones de identidad son verificadas: enlaces oficiales de la entidad
   o de sus ediciones, o ISRC exacto. No se asigna un Spotify ID por parecido de nombre.
   Si varias ediciones remiten a la misma entidad MB, se conserva una edición estable.
6. music_feature_aliases conserva equivalencias de IDs de Spotify a la misma entidad,
   para reutilizar características y excluir también las ediciones conocidas.
7. Los candidatos se preparan, pero no vuelven a expandirse automáticamente: no hay
   búsquedas recursivas. Las referencias con actividad positiva se pueden expandir
   de nuevo después de 7 días; el caché de fuentes también caduca después de 7 días.
8. Las correspondencias ausentes se omiten; no se inventan artistas/canciones.
   Los errores temporales se reintentan hasta 4 intentos. Un 429 activa pausa compartida.
   Los aplazamientos por presupuesto local no envían HTTP ni consumen intentos.
9. La cola admite 200 trabajos activos; utiliza bloqueo compartido y recuperación
   de trabajos interrumpidos después de 5 minutos. No guarda tokens ni cuentas.
10. Flutter consulta una vez al abrir. Solo Home permite Actualizar; los detalles
    no tienen refresh. No hay temporizadores ni recarga automática al volver.

MusicBrainz: 150 intentos diarios UTC y 2 segundos mínimos entre llamadas,
compartidos con los importadores manuales de Recomendaciones.
Spotify: conserva el cupo de 20 para verificación de identidad, independiente de
los 180 de navegación de Music. Las lecturas de caché no consumen presupuesto.
No se añaden llamadas a Groq para música.

Instalación existente: aplicar database/006_automatic_music_catalog.sql después de
las migraciones anteriores, conectado como propietario de recommendations_db.
Instalación nueva: setup.sql ya incluye cola musical y tabla de equivalencias.
La migración 006 ya se aplicó localmente, conservando el catálogo anterior.

Configuración opcional en .env:

```dotenv
MUSIC_CATALOG_AUTOPREPARE=true
MUSIC_CATALOG_CANDIDATES_PER_SEED=8
MUSICBRAINZ_DAILY_LIMIT=150
MUSICBRAINZ_INTERVAL_SECONDS=2
MUSIC_CATALOG_SPOTIFY_ENV=../music-service/.env
```

La última ruta es una comodidad local para la verificación de ISRC. En despliegue
se pueden inyectar SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET como secretos del servicio;
si no están disponibles, todavía pueden utilizarse las relaciones públicas verificadas.
Nunca se guarda el token Spotify en la cola ni en el caché de MusicBrainz.

Verificación real: se solicitó expansión desde Lush Life como señal positiva.
Se añadieron Zara Larsson, VENUS, Ain’t My Fault y A Brand New Day, además de un
artista de descubrimiento. Las correspondencias no verificables se omitieron y
una consulta con HTTP 503 quedó en cola. No se modificaron gustos ni calificaciones.
La prueba no sustituye la revisión de pertinencia de cada sugerencia con usuarios.
El catálogo público exportado se actualizó para compartir los elementos preparados.

Referencia de las consultas y relaciones: https://musicbrainz.org/doc/MusicBrainz_API

## Similares debajo del detalle musical

Las pantallas de artista, álbum y canción incluyen respectivamente «Artistas similares»,
«Álbumes similares» y «Canciones similares», debajo de información/reseñas. Cada tarjeta
abre otro detalle del mismo tipo. El elemento abierto y sus IDs equivalentes se excluyen.

GET /api/recommendations/music/{artists|albums|songs}/{external_id}/similar?limit=8
requiere sesión, valida tipo/ID y acepta de 1 a 20 resultados. Utiliza únicamente el
elemento abierto como referencia: no requiere calificarlo ni consulta los gustos de
la cuenta. Si falta prepararlo, encola su preparación con los presupuestos existentes.

Actualización en Home y presentación en los detalles:
- Una consulta al abrir la sección o cambiar usuario/referencia.
- Solo Home tiene Actualizar/Reintentar para volver a consultar los resultados.
- Los detalles muestran los similares disponibles al abrir; no tienen botones de refresh.
- Sin temporizador, sin recarga automática al volver de otro detalle.
- Pulsaciones repetidas durante una consulta en curso no generan otra solicitud.
- En Home el mensaje de preparación permite actualizar más tarde; los detalles no piden actualizar.
- La preparación ya encolada continúa en segundo plano con límites y deduplicación.
- Actualizar lee el catálogo; no fuerza una nueva extracción si ya está preparado.

No se necesitan tablas ni migraciones adicionales para estas rutas.
Se comprobaron las tres pantallas completas en móvil y escritorio y la ausencia
 de consultas nuevas tras tres minutos sin interacción.

## Páginas Music y Books conectadas al motor

MusicHomePage muestra recomendaciones reales al abrir, sin las tarjetas estáticas
anteriores de Explora para ti. Todo presenta artistas, álbumes y canciones; cada
filtro deja visible su categoría. Las secciones conservan su estado al filtrar:
no se repiten consultas del recomendador por pulsar otra pestaña. La búsqueda
conserva filtros para sus resultados y las reseñas se filtran por tipo.
BooksHomePage muestra recomendaciones personales en Todos y conserva las pestañas
de biblioteca/favoritos. Solo Home tiene controles de actualización.

## Portadas verificadas y caché

Una grabación Spotify puede aparecer en una recopilación y en el lanzamiento del
artista. Music detecta recopilaciones y álbumes de artistas ajenos, y hace una
búsqueda acotada por ISRC. Solo acepta la misma grabación y los mismos artistas,
con una portada de álbum/sencillo del artista; prefiere la edición más antigua de
las candidatas verificadas. Nunca elige una portada solo porque coincida el título.
Si no hay alternativa verificada, conserva la edición disponible.

La selección es presentación, no una característica ni una señal del KNN. No se
cambian IDs, notas, reseñas ni favoritos. artwork_checked_at indica que ya se revisó
la imagen y evita repetir la búsqueda en cada visita. Los registros anteriores
se revisan al cargarlos, respetando el presupuesto de Spotify. Si el proveedor
no está disponible, se conserva el dato local y puede revisarse más adelante.

El frontend obtiene la portada exacta del álbum Spotify en lugar de forzar una
del release-group de Cover Art Archive, que puede pertenecer a otra edición.
También conserva resultados negativos de imágenes durante la vida de la sección;
solo Actualizar en Home permite reintentar los fallidos. Cambiar filtros no los reconsulta.

Nueva migración de Music: database/migrations/003_original_artwork.sql. Ya aplicada
localmente; database/schema.sql también la incluye para instalaciones nuevas.
No requiere cambios de esquema para favoritos de libros: su tabla ya existía.

Comprobación real acotada: 1 step forward, 3 steps back pasó de Pop en Inglés de
Desamor a SOUR; Dividido pasó de Música de Fiesta para Bailar a 11 RAZONES. Se
verificó la identidad antes de guardar las dos imágenes y se conservaron los IDs.
Referencia de campos y filtro ISRC: https://developer.spotify.com/documentation/web-api/reference/search
