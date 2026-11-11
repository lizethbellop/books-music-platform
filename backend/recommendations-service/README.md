# Recomendaciones de musa. — Goodbooks + OpenLibrary

Servicio Python de entrenamiento offline e inferencia REST con JWT. Usa
**OpenLibrary**, igual que `books-service/OpenLibraryService.java`: `externalId`
es un **Work ID `OL…W`**, sin el prefijo `/works/`. No usa Google Books.
La referencia a Google Books en la primera entrega era incorrecta.

El notebook `notebooks/01-book-explore.ipynb` permanece intacto y el README
experimental original se conserva en `docs/EXPERIMENTS.md`.

## Integración actual en Home (sólo libros)

Flutter muestra **Lecturas para ti dentro de Inicio**, en el espacio sin posts.
`GET /api/recommendations/books/me` obtiene las listas privadas de la sesión,
recupera candidatos OpenLibrary y combina contenido con KNN cuando hay equivalencia.
No requiere que el cliente mande semillas ni UUID, y no está limitado a 16 obras.

Ver **[docs/HOME.md](docs/HOME.md)** para el flujo, configuración, arranque, límites
y tests. Los apartados siguientes documentan el entrenamiento offline y el endpoint
POST de diagnóstico que también se conserva. Sus métricas no validan el ranking
online nuevo. No se implementó integración de música en Home.

## Qué hace cada archivo

| Archivo | Responsabilidad |
| --- | --- |
| `recommender/audit.py` | Revisa todos los CSV sin modificarlos; cuenta duplicados y valida ISBN |
| `recommender/prepare_goodbooks.py` | Produce catálogo y ratings normalizados |
| `recommender/data.py` | Valida entradas, promedia pares duplicados y separa train/validación/test |
| `recommender/model.py` | Entrena vecinos, contenido y factorizaciones; calcula recomendaciones |
| `recommender/train.py` | Compara candidatos, elige con validación, mide test y guarda modelo final |
| `recommender/openlibrary.py` | Resuelve ISBN -> Work ID con evidencia, sin inventar equivalencias |
| `recommender/artifacts.py` | Persiste/carga JSON y NPZ con hashes; no ejecuta pickle |
| `recommender/home.py` | Lee listas por APIs y recupera/ordena obras para Inicio |
| `recommender/serve.py` | Arranca con configuración local y JWT existente |
| `recommender/api.py` | Valida JWT y devuelve sólo obras OpenLibrary verificadas cuando hay crosswalk |
| `tests/` | Pipeline, API, seguridad, ISBN, ambigüedad y exclusión de ediciones equivalentes |

## Datos proporcionados

Auditoría de la carpeta `raw` indicada por la usuaria:

| Archivo | Filas | Uso en esta versión |
| --- | ---: | --- |
| `books.csv` | 10,000 | Título/autores para contenido; ISBN para equivalencias |
| `ratings.csv` | 981,756 | Calificaciones explícitas 1..5 |
| `book_tags.csv` | 999,912 | Se audita; no se usa como señal de entrenamiento |
| `tags.csv` | 34,252 | Se audita; vocabulario de etiquetas comunitarias |
| `to_read.csv` | 912,705 | Se audita; estar en una lista no equivale a una calificación positiva |

Hay 53,424 usuarios, 2,278 filas repetidas por par usuario/libro y 633 pares con
ratings distintos. Se promedian y quedan **979,478 pares**. No hay duplicados en
`books.work_id` ni referencias de ratings fuera del catálogo. El XML es un ejemplo,
se conserva y no se ejecuta ni se incorpora como interacciones.

`book_tags.csv` se une con `books.book_id` mediante `goodreads_book_id`, mientras
que `ratings.book_id` apunta a `books.id`. Son dos identificadores diferentes.
`books.work_id` es un ID de **Goodreads**, tampoco un ID de OpenLibrary.

No se usan `average_rating`, `ratings_count`, frecuencias de tags ni otras señales
comunitarias globales: pueden contener comportamiento posterior al holdout. Sin
snapshots/timestamps no podemos asegurar su procedencia temporal. Tampoco se mezcla
`to_read.csv` con ratings de 5; ese objetivo implícito requiere otro diseño.

## OpenLibrary: la equivalencia entre catálogos

```text
ratings.book_id -> books.id -> ISBN válido -> obra OpenLibrary OL...W
    ID interno de Goodbooks                  externalId que espera musa.
```

OpenLibrary diferencia obras (Work, W) y ediciones (Edition, M). El ISBN identifica
una edición; el servicio de musa. navega por obras. Un mapeo a `OL...M` sería
incompatible con `/works/{externalId}.json`.

Documentación oficial:
- https://openlibrary.org/dev/docs/api/books
- https://openlibrary.org/dev/docs/api/search
- https://openlibrary.org/developers/dumps
- https://openlibrary.org/developers/api

`openlibrary.py` valida checksum ISBN-10/13, recupera un cero inicial perdido en
ISBN-10 de nueve dígitos y convierte a ISBN-13. Trata notación científica mediante
Decimal, sin redondeos de float. Si el valor ya perdió precisión y no pasa checksum,
lo rechaza. No adivina el último dígito. En estos CSV **8,371 libros** tienen algún
ISBN válido; **1,629** necesitan enriquecimiento o revisión adicional.

El crosswalk CSV registra por cada ID: obra, estado, ISBN y evidencia/fuente.
Estados: `matched`, `ambiguous`, `unresolved`, `invalid_isbn`. Un ISBN que conduce
a varias obras no se asigna automáticamente. Ni el título ni un ID numérico de
Goodreads se convierten por intuición en un Work ID de OpenLibrary.

### Prueba pequeña en vivo

```bash
python -m recommender.openlibrary --books "$RAW/books.csv" \
  --lookup-sample 20 --output data/processed/openlibrary-crosswalk.csv
```

Consulta Search API por lote de ISBN, conserva respuesta original con URL/fecha y
hash para repetir el procesamiento sin otra llamada. `--lookup-sample` está acotado
a 1..50 libros y rechaza resultados truncados. El resultado de esta entrega fue
**16 coincidencias y 2 casos ambiguos**; los restantes del lote no se resolvieron.
La tabla incluye todos los libros, pero sólo esos 16 quedan habilitados para servir.

**Esto no equivale a tener los 10,000 libros enlazados al grafo KNN.** El modelo recomienda sobre
Goodbooks; el POST de diagnóstico devuelve sólo obras verificadas y reporta cobertura.
El GET de Home agrega recuperación nativa de OpenLibrary para obras no mapeadas.
Los resultados offline sobre 10,000 libros no describen el ranking restringido a 16.

### Mapeo completo desde un dump local

La herramienta procesa el dump oficial de ediciones de OpenLibrary en streaming,
TSV o TSV.gz; busca los ISBN del catálogo y extrae `works[].key`. No carga el dump
completo en RAM. También acepta JSONL para snapshots pequeños y pruebas.

```bash
python -m recommender.openlibrary --books "$RAW/books.csv" \
  --editions-dump /ruta/ol_dump_editions_FECHA.txt.gz \
  --output data/processed/openlibrary-crosswalk-completo.csv
```

El dump no está incluido en los archivos proporcionados y puede ser grande.
No se descargó durante esta entrega. No se lanzan miles de consultas individuales:
OpenLibrary pide usar sus dumps para trabajo masivo. Los ISBN sin coincidencia y
los casos ambiguos requieren revisión; no se promete cobertura total ni siquiera
con un dump completo. La API no consulta OpenLibrary durante la recomendación.

## Modelos y decisión

| Candidato | Señal | Limitación |
| --- | --- | --- |
| Popularidad | Cantidad de interacciones con rating >=4 | Sin personalización |
| Item-KNN | Coseno entre vectores de usuarios que disfrutaron cada libro | Depende de interacciones; sufre con ítems nuevos |
| MF explícita | Media global + sesgos + factores, como notebook | La inferencia de usuarios nuevos usa fold-in aproximado |
| Híbrido | 55% TF-IDF de título/autores + 35% KNN + 10% popularidad | Pesos iniciales; contenido bibliográfico limitado |
| NMF opcional | Factores no negativos sobre positivos | Objetivo distinto y coste mayor; `--include-nmf` |

KNN usa 40 vecinos por ítem. La similitud se precalcula por lotes de 128 y se
conserva en una matriz sparse O(items*k); no se almacena la matriz completa items².
El cálculo exacto sigue siendo costoso al crecer el catálogo. La inferencia no
reentrena ni construye vecinos. Puntúa el catálogo, ordena y excluye semillas/vistos.

La MF replica la ecuación SGD del notebook: 20 factores, 3 épocas, learning rate
.01 y regularización .02. Para una sesión nueva estima un vector a partir de los
factores de libros semilla con ridge .1, asumiendo rating 5 para esos positivos.
No reutiliza al usuario de Goodbooks ni convierte un UUID musa. en ID del dataset.
Este contrato de sesión nueva es diferente del RMSE de usuarios ya conocidos del
notebook (sus salidas guardadas: RMSE .8956, MAE .7139). No son métricas comparables.

En esta ejecución real ganó **Item-KNN** en validación. Es una elección inicial
para estos datos y este contrato; no prueba que KNN supere cualquier factorización.
NMF no se ejecutó en el entrenamiento real de esta entrega: queda como opción y
se verifica con el fixture de tests. No se ocultó bajo la etiqueta MF explícita.

Cold start: sin semillas conocidas se devuelve popularidad y se informa cuáles no
están mapeadas. El híbrido puede puntuar ítems sin ratings por contenido, pero no
fue el ganador actual y no se activa silenciosamente. Ítems externos al catálogo
entrenado requieren actualizar el catálogo y crear otro artefacto.

## Protocolo de evaluación

- Se promedian duplicados ANTES de dividir, para evitar el mismo par en particiones distintas.
- Semilla 42; un par de validación y otro de test por usuario con >=3 pares. El resto va a train.
- Ratings >=4 cuentan como positivos para ranking; todos los vistos se excluyen.
- Usuarios con <3 pares sólo entrenan. No se filtra usando frecuencias del holdout.
- Catálogo de metadatos estáticos conocido: título y autores. Interacciones, popularidad,
  vecinos, sesgos y factores se ajustan sólo con train al comparar candidatos.
- Se fija el algoritmo por NDCG@10 de validación, se reajusta con train+validación
  y se mide test una vez junto al baseline predefinido.
- Se reajusta con todos los pares para servir. Ese modelo final no se evalúa sobre
  datos ya incorporados; las métricas pertenecen al modelo anterior de evaluación.

Por coste, el CLI selecciona **1,000 usuarios uniformemente sin reemplazo** de los
45,071 elegibles, antes de comprobar si su target es positivo. Usa la misma lista
en validación y test. El entrenamiento usa TODOS los pares correspondientes,
no sólo esos usuarios. `--eval-users 0` evalúa todos los elegibles.
Se reporta el denominador positivo de cada partición; no confundirlo con los 1,000.

El split es aleatorio por pares, **no temporal**. Los CSV no incluyen fechas.
No demuestra calidad sobre eventos futuros ni sobre usuarios de musa. Con eventos
propios, pasar a cortes temporales globales y snapshots del catálogo, y evaluar
segmentos frío/caliente. Para concluir superioridad: otras semillas/particiones,
intervalos por usuario y prueba online; no ajustar con el test aquí reportado.

## Instalación y reproducción

Python 3.11/3.12; esta entrega se ejecutó con Python 3.12.14. Desde este directorio:

```bash
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.lock
python -m pytest -q

export RAW='/Users/lizbello/Documents/7mo Semestre/books-music-platform/backend/recommendations-service/data/raw'
python -m recommender.audit --raw "$RAW" --output data/processed/raw-audit.json
python -m recommender.prepare_goodbooks --raw "$RAW" --output data/processed
# Crear equivalencias con una de las opciones descritas antes.
python -m recommender.train --catalog data/processed/catalog.csv \
  --ratings data/processed/ratings.csv --namespace goodbooks --domain books \
  --crosswalk data/processed/openlibrary-crosswalk.csv --eval-users 1000 \
  --output artifacts/books-openlibrary-v2
```

El output debe ser una carpeta NUEVA. Para repetir, elegir otra versión.
Formato normalizado: catálogo `item_id,title,authors,tags`; ratings
`user_id,item_id,rating`. Se rechazan ratings no finitos/fuera de 1..5, referencias
inexistentes, catálogos vacíos o IDs duplicados. `examples/` es sólo un fixture.

## Artefactos, API y Gateway

Manifest registra versión, esquema, configuración, hashes de entradas y código,
versiones de librerías, métricas, muestra de evaluación y cobertura del crosswalk.
JSON/NPZ no ejecutan pickle. SHA-256 verifica integridad, no autentica una fuente
maliciosa. Montar sólo artefactos de confianza, completos y de sólo lectura.
No se guardan tokens ni IDs de usuarios en el modelo. Factores y vecinos siguen
siendo datos derivados de comportamiento y deben protegerse.

```bash
export MODEL_PATH=artifacts/books-openlibrary-v2
# Cargar JWT_SECRET desde gestor de secretos: la misma clave Base64 de users-service.
uvicorn recommender.api:app --host 0.0.0.0 --port 8087
```

JWT: HS512, clave >=64 bytes decodificados, issuer `musa-auth`, audience `musa-api`,
exp obligatoria y sub UUID; se valida firma y tiempos. No se confía en X-User-Id
ni se acepta consultar historial ajeno por ID. Las limitaciones de revocación
stateless son las mismas de los demás resource servers.

Gateway reenvía `/api/recommendations/**` a
`${RECOMMENDATIONS_SERVICE_URL:http://localhost:8087}` sin cambiar ruta ni Bearer.

```bash
curl -X POST http://localhost:8084/api/recommendations/books \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"seeds":["OL123W"],"exclude":[],"limit":5}'
```

`OL123W` es un ejemplo de formato; usar los Work IDs efectivamente mapeados.
Se aceptan también `/works/OL...W`, normalizados a OL...W. La respuesta contiene
`model_version`, `namespace=openlibrary-work`, `mapped_items`, `total_items`,
`unmapped_seeds` e `items` con `item_id`, `externalId`, `goodbooks_id`, título,
score y reason. `externalId` es lo que se pasa a `GET /books/{externalId}`.
Si varias entradas Goodbooks corresponden a la misma obra, se devuelve una sola;
semillas/exclusiones bloquean todas sus equivalencias. Nunca se expone como
externalId un ID sin correspondencia verificada. Sin ninguna equivalencia: 503.

JWT inválido: 401. Payload/IDs inválidos: 422. Dominio no cargado: 404.
Límite 1..100, 100 semillas y 1000 exclusiones. `/health/live` y `/health/ready`
son sondas internas; OpenAPI en `/docs`. No hay secretos por defecto. Dockerfile
usa usuario sin privilegios. No se construyó imagen en esta entrega.

El frontend/BFF debe obtener historial propio con JWT: positivos como semillas,
TODOS los vistos como exclusiones. No se implementó aquí un consumidor de eventos
ni se modificó frontend o bases de datos. Configurar TLS, límites y política CORS
existente del Gateway para despliegue. No abrir directamente el puerto de servicio.
Promover artefacto completo tras validar métricas; reiniciar con nuevo MODEL_PATH.
Rollback: volver a una versión anterior. No hay recarga ni entrenamiento en peticiones.

## Música después

Mismo contrato con `--domain music --namespace spotify-track`, `item_id=spotifyId`,
título, artista en authors y géneros en tags. Hace falta un snapshot real con
ratings 1..5; no se convierten plays en ratings sin diseñar el objetivo. Separar
canciones, álbumes y artistas por tipo. Una instancia sirve un dominio; para ambos
simultáneos desplegar otra instancia y una ruta específica de Gateway. El modelo
libros devuelve 404 para música. Crosswalk Goodbooks sólo se permite para libros.
