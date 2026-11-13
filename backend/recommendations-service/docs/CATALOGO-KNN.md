# Catálogo, KNN y gustos

Implementación del motor por características. El catálogo se prepara de forma
controlada. El ranking no llama a Groq; las consultas encolan preparación
automática en segundo plano cuando falta información.

## 1. Crear o actualizar la base

Las actualizaciones están en `database/002_catalog_metadata.sql` y
`database/004_automatic_book_catalog.sql` (cola y presupuesto compartido).

Conéctate a `recommendations_db` como su propietario y ejecuta este archivo.
No vuelvas a ejecutar la creación del usuario y la base. En instalaciones nuevas,
`setup.sql` incluye todas las tablas; no hace falta repetir migraciones.

La columna `metadata` conserva título, autores, portada y fecha para que KNN pueda
devolver tarjetas sin consultar OpenLibrary durante cada recomendación. No guarda
tokens, reseñas personales ni información de cuentas.

## 2. Componentes instalados

- `book_features_repository.py`: guarda características y metadatos; consulta catálogo vigente.
- `service/book_features_service.py`: reutiliza o extrae características de libros.
- `service/book_tastes.py`: consulta gustos propios y aplica prioridades/exclusiones.
- `service/book_knn.py`: vectorización, vecinos, ranking y diversidad.
- `service/book_recommendations_service.py`: coordina gustos y catálogo.
- `service/book_catalog_jobs.py`: cola persistente, trabajador, candidatos y presupuesto.
- `prepare_catalog.py`: preparación manual opcional limitada por cantidad.
- Books: `BookRecommendationSignalsController`, ruta `/books/recommendation-signals/me`.

## 3. Endpoint del nuevo motor

`GET /api/recommendations/books/knn/me?limit=12`
Header: `Authorization: Bearer <token de sesión>`.

Home ya utiliza esta ruta. El endpoint anterior se conserva para compatibilidad. El gateway existente reenvía `/api/recommendations/**`.
Los controles de caché son `no-store` y `Vary: Authorization`.

## 4. Configuración

Usa las variables `RECOMMENDATIONS_DB_*`, `PROFILE_SERVICE_URL`,
`BOOKS_SERVICE_URL`, `GROQ_MODEL`, `GROQ_API_KEY` y la configuración JWT habitual.
No hay contraseñas ni claves incluidas en estos archivos.

## 5. Preparación automática y herramienta manual opcional

Home y el detalle ya encolan la preparación de referencias y candidatos.
Consultar INICIO-RECOMENDACIONES.md para el flujo, estados y límites.
El siguiente comando sirve para importaciones explícitas; no es un requisito
para cada libro que se abra en la aplicación.

Primero arrancar Books y tener un token válido. PostgreSQL debe estar disponible.
Desde `backend/recommendations-service`:

```bash
.venv-service/bin/python -m recommender.prepare_catalog --ids OL26350090W --query 'subject:"women in stem" OR subject:"enemies to lovers"' --max-books 5
```

El token se pide de forma oculta. No lo escribas en archivos ni lo compartas.
Este comando prepara el libro explícito y hasta cuatro candidatos de la búsqueda.
Las semillas nuevas también se preparan automáticamente al consultar Home.
Puedes usar varios IDs con `--ids OL26350090W OTRO_ID_REAL ...` sin búsqueda.
Las búsquedas vacías o sin resultados pueden devolver pocos candidatos.

Cada libro requiere consultar sus metadatos; solo si su extracción está ausente
o cambió llama a Groq. Una espera de 20 segundos separa los libros, pero no garantiza
no agotar cuotas. Al recibir un error HTTP se detiene sin reintentos automáticos.
Lo ya guardado se conserva; repetir reutiliza extracciones vigentes.
Las extracciones concurrentes del mismo libro se coordinan con un bloqueo en
PostgreSQL. El presupuesto compartido también se aplica a este comando. No
ejecutar el importador al cargar Home: la aplicación utiliza la cola automática.

## 6. Reglas de gustos

- Listas y preferencias: peso 1, sin duplicar un libro por aparecer varias veces.
- Calificación de 4 a 5: señal positiva, peso de 2 a 3.
- Una nota inferior a 4 elimina al libro como semilla, aunque esté en una lista.
- Se excluyen de resultados todos los libros en listas, preferencias, biblioteca
  o con calificación, incluyendo los que no gustaron.
- Estar únicamente en biblioteca no implica que te gustó: sirve como exclusión.
- Favoritos explícitos de Books: señal positiva de peso 1.5. Una nota menor de 4
  prevalece y elimina ese libro como semilla aunque esté marcado como favorito.
- No se almacenan ni cachean los gustos personales en este catálogo compartido.

## 7. Cómo trabaja KNN

Vecinos entre libros según características extraídas, con distancia coseno.
Grupos iniciales: temas 3, géneros 2, tropes 2, entorno 1.5, público 0.5 y formato 0.5.
Son decisiones iniciales de diseño, no pesos demostrados como óptimos.
Una coincidencia solo de público o formato no basta para recomendar.
La mejor afinidad ponderada con una semilla determina la puntuación.
No es una probabilidad de que el libro guste.
Se diversifica por primer autor con un máximo suave de dos antes de rellenar.
Las explicaciones indican características compartidas, no causas inventadas.

No se usa autor como característica KNN en esta primera versión; sí para diversificar.
La extracción trabaja con temas, géneros, sinopsis y tropes, como acordamos.
El vocabulario es coincidencia de etiquetas normalizadas: no entiende por sí solo
que dos sinónimos diferentes son equivalentes. La consistencia del extractor debe
evaluarse; más adelante se pueden añadir equivalencias o embeddings.

Se construye el índice en memoria en cada consulta, apropiado para el catálogo
inicial pequeño. No se entrena una red ni se llama a Groq en el ranking. Un catálogo
grande requerirá guardar/reutilizar un índice y limitar el trabajo por petición.

## 8. Qué responde si falta información

- `catalog_not_ready`: no hay características utilizables en el catálogo.
- `profile_not_prepared`: hay gustos, pero ninguna semilla está preparada.
- `explore`: la cuenta no tiene señales positivas; descubrimiento sin personalización.
- `knn_content`: ranking personalizado con características.
- `partial=true` y `missing_seed_ids`: faltan algunas semillas; usa las disponibles.

La respuesta añade `preparation` para indicar preparación pendiente. El trabajador
puede consultar Groq en segundo plano; el ranking devuelve lo disponible sin esperar.
Los fallos de Profile/Books no se convierten en un perfil vacío.

## 9. Validación

Ejecutar desde `backend/recommendations-service`:

```bash
.venv-service/bin/python -m pytest -q tests/test_book_knn_tastes.py
```

Validación realizada en esta implementación:
- Python: 111 pruebas aprobadas, incluidas 20 optativas con PostgreSQL aislado.
- Books: 19 pruebas aprobadas, incluyendo la ruta de señales con JWT.
- PostgreSQL: inserción, consulta, actualización y filtro de versiones comprobados
  con una tabla temporal, sin modificar los registros reales.
- Migración local aplicada; el registro existente se conservó.

Las pruebas no realizaron llamadas a Groq ni intentos de login.
No sustituyen una sesión real con catálogo preparado y los servicios encendidos.

Para comprobar la integración reiniciar Books tras añadir el controlador,
arrancar Profile y el recomendador con su configuración habitual.

Psycopg y su distribución binaria están fijados en requirements.lock.
Nunca versionar .env ni tokens de sesión.

## Alcance pendiente después de estos dos puntos

En cada instalación aplicar las migraciones y validar el flujo autenticado.
Ampliar el catálogo y medir pertinencia con ejemplos reales.
Se añadieron presupuesto global de Groq y deduplicación compartida. Para un
despliegue público aún conviene aplicar límites de solicitudes por cuenta en el gateway.
Home ya está conectado con carruseles de libros, artistas, álbumes y canciones.
Consultar INICIO-RECOMENDACIONES.md para instalación y comprobación conjunta.

## Personalización en Books (política v3)

BooksHomePage consume el mismo endpoint personal que Home al abrir Todos; ya no
muestra una búsqueda genérica de OpenLibrary como si fuera una recomendación.
Las pestañas de biblioteca y favoritos conservan su función. La pantalla no tiene
refresh ni temporizadores; solo Home permite actualización manual.

La ruta de señales de Books devuelve favoriteIds además de notas y biblioteca,
siempre para el UUID del token. Los favoritos aportan peso 1.5 y se excluyen como
conocidos. Las notas inferiores a 4 prevalecen sobre listas y favoritos.
La política book-features-knn-v3 atenúa categorías amplias como fiction/nonfiction
 y utiliza IDF; compartir solo una categoría amplia no basta para recomendar.
No necesita volver a extraer características ni consumir tokens por este cambio.
