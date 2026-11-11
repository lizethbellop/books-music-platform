# Recomendaciones de libros en Inicio

La integración está en el **Home existente** de Flutter. Reemplaza el bloque
"Todavía no hay contenido" mientras no hay posts; no agrega página, pestaña ni ruta
de navegación de recomendaciones. Las tarjetas abren `BookDetailPage` con el
`externalId` OpenLibrary real y el usuario de la sesión actual.

## Recorrido completo

```text
Home -> Gateway GET /api/recommendations/books/me + Bearer
        -> recommendations-service valida el JWT
           -> profile-service GET /api/profiles/me/lists/book-seeds + MISMO Bearer
           -> books-service obtiene metadatos y candidatos OpenLibrary
           -> ranking de contenido + KNN si existe correspondencia verificada
        <- tarjetas con portada, autor y una explicación breve
```

El cliente no envía un userId ni escoge manualmente libros semilla. Profile obtiene
el UUID del JWT. La nueva consulta de repositorio devuelve sólo referencias BOOK
del perfil autenticado, deduplicadas entre listas; excluye música y listas ajenas.
No resuelve los detalles de cada elemento ni necesita invocar music-service.
Si aún no existe perfil, devuelve una colección vacía; errores del servicio no se
confunden con listas vacías.

La integración toma **las listas personales y las preferencias BOOK de Mi perfil**. Biblioteca,
Favoritos y estados de lectura del servicio de libros son otras fuentes posibles;
no se mezclan automáticamente con estas listas en esta versión.

## De una lista a otras lecturas

1. Lee las referencias de TODOS los libros de las listas y preferencias. Nunca se guardan en una
   caché compartida; volver a Inicio o actualizar vuelve a consultar las listas.
2. Elige hasta tres obras con un muestreo determinista por hash para acotar llamadas.
   TODOS los libros guardados siguen siendo exclusiones, incluso los no muestreados.
3. Obtiene título, autores y temas de esas obras mediante books-service.
4. Construye una búsqueda de hasta tres temas y dos autores. El nuevo endpoint
   `/books/recommendation-candidates?q=...` hace una sola búsqueda OpenLibrary con
   límite 30 y campos explícitos. A diferencia de la búsqueda de catálogo normal,
   no consulta ediciones una por una para completar portadas faltantes.
5. Transforma el texto con HashingVectorizer fijo, sin `fit` ni reentrenamiento en
   peticiones. Calcula coseno entre candidatos y libros guardados; usa la mayor
   afinidad a una de las semillas para que una lista mixta conserve varios intereses.
6. Si hay semillas con equivalencias Goodbooks/OpenLibrary, consulta el grafo KNN
   ya entrenado y añade hasta tres vecinos verificados. Combina 70% contenido y
   30% señal colaborativa normalizada; sin KNN usa sólo contenido.
7. Deduplica por Work ID, excluye todos los libros guardados y diversifica autores
   con un máximo suave de dos por autor antes de rellenar huecos.

La política online se identifica como `home-books-v1`. Sus pesos y representación
son una primera política de integración, no parámetros optimizados con los ratings
de las listas. Las métricas offline de Goodbooks del modelo `books-openlibrary-v2`
NO validan este recuperador/reranking online. Antes de afirmar mejora, recopilar
feedback de musa. y evaluar este flujo concreto.

La vía de contenido trabaja directamente con obras de OpenLibrary. **Home ya no
está restringido a las 16 equivalencias iniciales.** Esa limitación sólo afecta a
la señal KNN y al antiguo endpoint POST de diagnóstico. No se inventan equivalencias
entre catálogos para recomendar libros sin correspondencia.

## Arranque local

Necesita los servicios habituales de musa. (users para login, profile, books y
Gateway), más el recomendador en 8087. Hay que reiniciar/recompilar profile, books,
Gateway y Flutter tras incorporar estos cambios; una instancia Java antigua no
contiene los endpoints nuevos. No cambian el esquema ni las migraciones de datos.

Desde `backend/recommendations-service`, Python 3.11 o 3.12:

```bash
python3.12 -m venv .venv-service
source .venv-service/bin/activate
pip install -r requirements.lock
cp .env.example .env
python -m recommender.serve --auth-env ../books-service/.env
```

El arranque lee únicamente JWT_SECRET del archivo de books si no está ya configurada
en el entorno o en la configuración del recomendador. No imprime la clave ni importa
credenciales de base de datos. La clave debe coincidir con la que usa users-service
para firmar y con la que books/profile usan para verificar.

`.env.example` apunta a `artifacts/books-openlibrary-v2`, el modelo real entrenado
en la entrega anterior. Si ese artefacto no está copiado, omitir MODEL_PATH o dejarla
vacía permite arrancar sólo con contenido. Un MODEL_PATH configurado pero inválido
falla al arrancar; no se ignoran silenciosamente artefactos corruptos.

`PROFILE_SERVICE_URL` y `BOOKS_SERVICE_URL` son los orígenes de los servicios Java,
por defecto `http://localhost:8082` y `http://localhost:8081`. En contenedores usar
los nombres internos correspondientes. No agregar `/api/profiles` o `/books` a estas
variables. Gateway usa RECOMMENDATIONS_SERVICE_URL o localhost:8087. Flutter usa el
GATEWAY_URL ya centralizado; no necesita apuntar al puerto Python directamente.
Gateway permite CORS de desarrollo para la nueva ruta desde localhost/127.0.0.1
con cualquier puerto. En web desplegada, configurar MUSA_WEB_ORIGIN con el origen
HTTPS exacto de la app. El preflight OPTIONS no exige token; el GET sí valida JWT.
No abrir directamente el servicio Python al navegador ni agregar CORS público allí.

## Contrato de Inicio

```http
GET /api/recommendations/books/me?limit=12
Authorization: Bearer <access token de musa.>
```

`limit` admite 1..20. Respuesta:

```json
{
  "policy_version": "home-books-v1",
  "model_version": "books-openlibrary-v2",
  "namespace": "openlibrary-work",
  "seed_count": 3,
  "mode": "content",
  "partial": false,
  "items": [
    {
      "externalId": "OL123W",
      "title": "Ejemplo de formato",
      "authors": ["Autora"],
      "subjects": ["Fantasy"],
      "coverUrl": null,
      "firstPublishYear": null,
      "score": 0.42,
      "reason": "Porque guardaste «Otro libro»"
    }
  ]
}
```

El JSON es un ejemplo de formato, no un resultado real. Sin libros en listas:
`mode=explore`, `seed_count=0`; Home invita a guardar libros y muestra descubrimiento
sin afirmar personalización. Con listas y candidatos genuinamente vacíos: estado
vacío que enlaza a Mis listas. Con fallos: reintento, sin inventar recomendaciones.
Si algunos metadatos fallan pero hay señal suficiente, devuelve `partial=true` y
Home informa la carga parcial. Si no puede consultar el perfil o ninguna semilla
utilizable, devuelve 503. No devuelve un catálogo genérico fingiendo basarse en listas.

El JWT se verifica en Python y se propaga a las APIs Java, que vuelven a comprobar
sus permisos. Las respuestas personales llevan `Cache-Control: no-store` y
`Vary: Authorization`. Sólo se cachean metadatos públicos de libros/búsquedas,
máximo 256 entradas durante 10 minutos, nunca listas, tokens ni rankings personales.
La caché es local al proceso; no comparte estado privado entre cuentas.

Hay hasta tres peticiones concurrentes de metadatos; un máximo de tres semillas,
una búsqueda y tres vecinos extra por petición, sin consultas por miles de libros.
Timeout HTTP 6 s y presupuesto total 18 s. La app espera hasta 22 s y permite
reintentar. No es un SLA: medir con los servicios reales, red y cuotas del proveedor.

## Validación

- Python: autenticación, límite, propagación del Bearer, obras no mapeadas,
  exclusiones completas, listas vacías, fallos, aislamiento entre usuarios y cambios
  de listas; además de los tests offline anteriores.
- Profile: MockMvc comprueba identidad del JWT; H2 ejecuta la consulta real y verifica
  exclusión de otro perfil/música y deduplicación entre listas.
- Books: prueba de cliente OpenLibrary comprueba una búsqueda y ausencia de fan-out
  para portadas; pruebas de seguridad protegen el endpoint nuevo.
- Gateway: prueba HTTP real con upstream de prueba comprueba ruta, Bearer y 401.
- Flutter: Home, móvil/escritorio, retry, estados, apertura del detalle y navegación.

Los tests con upstreams simulados verifican contratos y comportamiento; no equivalen
a una sesión real con PostgreSQL, login y todos los servicios desplegados juntos.
La vista PNG de entrega usa datos de prueba para inspeccionar el diseño.
