> Esta explicación documenta el entrenamiento y la API manual de la entrega anterior.
> La integración actual en Inicio, basada automáticamente en listas, está en [HOME.md](HOME.md).
> Home agrega recuperación nativa de OpenLibrary y ya no se limita al cruce inicial de 16 obras.

# Qué estamos construyendo y por qué

La recomendación tiene dos problemas distintos: **elegir un libro que podría
gustarte** y **saber cuál es ese libro en la API que ya usa musa.** Entrenar un
modelo resuelve el primero; la equivalencia con OpenLibrary resuelve el segundo.

## 1. Tus CSV enseñan gustos; OpenLibrary identifica y describe obras

`ratings.csv` dice qué usuario de Goodbooks calificó qué libro y con qué nota.
Tiene 981,756 filas; al consolidar repetidas quedan 979,478 pares de 53,424 usuarios.
`books.csv` contiene 10,000 libros con título, autor e ISBN.

Los otros archivos también fueron revisados. No todo archivo debe entrar al modelo:

- `book_tags.csv` y `tags.csv` contienen etiquetas comunitarias. Sus frecuencias
  pueden incluir actividad del conjunto de prueba. En esta primera evaluación
  usamos título/autor como contenido y reservamos las etiquetas para un experimento
  con procedencia temporal controlada.
- `to_read.csv` contiene intenciones de lectura. Guardar un libro no significa
  haberlo disfrutado ni ponerle un 5. Mezclar esas filas con ratings alteraría el
  significado de los datos.
- `sample_book.xml` es un documento de ejemplo, no una tabla de interacciones.

OpenLibrary sigue proporcionando búsqueda, detalle y portadas mediante el servicio
Java existente. El recomendador no reemplaza ese servicio ni necesita llamar a
OpenLibrary cada vez que alguien solicita una lista.

## 2. Un mismo libro tiene varios identificadores

En tus CSV, `ratings.book_id=1` apunta a `books.id=1`: The Hunger Games.
Su ISBN-10 aparece como `439023483`: al perder el cero inicial, hay que recuperarlo
como `0439023483` y validar su checksum. Su ISBN-13 correcto es `9780439023481`.
La variante redondeada de notación científica del CSV falla checksum y se descarta.

La consulta real por ISBN enlazó ese libro con **`OL5735363W`**. Ése es el
`externalId` que tu `OpenLibraryService` entiende. No se puede mandar simplemente
`1`: ese número pertenece al catálogo Goodbooks.

OpenLibrary distingue una obra (W) de sus ediciones (M). El endpoint Java consulta
`/works/{externalId}.json`; por eso necesitamos la obra, aunque el ISBN venga de
una edición. El código acepta únicamente Work IDs válidos, registra evidencia y
rechaza equivalencias ambiguas.

La prueba en vivo resolvió 16 de un lote de 20 libros. No se enlazó el catálogo
completo: 1,629 de los 10,000 libros carecen de un ISBN válido utilizable y requieren
enriquecimiento. Para procesar masivamente el resto, `openlibrary.py` ya importa
el dump oficial local de ediciones. No se descargó ese archivo grande ni se hizo
una carga masiva contra la API pública.

## 3. Qué aprende KNN aquí

No usamos KNN para clasificar un libro en una categoría. Usamos **vecinos entre
libros**: dos libros se consideran similares si varios usuarios disfrutaron ambos.

Para cada libro construimos un vector con los usuarios que le pusieron 4 o 5.
Calculamos similitud coseno y guardamos sus 40 vecinos más cercanos. Para alguien
que elige The Hunger Games como semilla, buscamos candidatos cercanos a ese libro;
con varias semillas combinamos sus vecinos. Una pequeña señal de popularidad
ayuda a ordenar candidatos con poca evidencia.

Los vecinos se calculan al entrenar, no en cada petición. El modelo final conserva
relaciones entre libros, factores y metadatos; no necesita llevarse los IDs de los
usuarios de Goodbooks a la API.

## 4. Por qué no elegimos el modelo sólo por intuición

Comparamos popularidad, KNN, la MF explícita del notebook y un híbrido de KNN con
contenido y popularidad. La NMF implícita existe como opción adicional, pero no
se ejecutó en la corrida real; sería incorrecto presentar sus resultados sintéticos
como si fueran datos de tus CSV.

Antes de entrenar, se separaron pares usuario/libro. Cada usuario elegible conserva
un par para validación y otro para test. Ningún par aparece en dos particiones.
El notebook original permanece intacto; el código nuevo automatiza este proceso.

- **Train:** aprende similitudes, sesgos y factores.
- **Validación:** elige el algoritmo.
- **Test:** mide el algoritmo elegido, sin volver a escoger según su resultado.

Entrenamos con todos los pares de cada partición. Para acotar el coste de evaluar,
seleccionamos 1,000 usuarios elegibles con semilla fija, antes de ver si el objetivo
reservado era positivo. Hubo 654 con positivo en validación y 671 en test.
No son 1,000 usuarios de entrenamiento: el entrenamiento sí usa todos los demás.

### Resultados reales de validación

| Candidato | Recall@10 | NDCG@10 |
| --- | ---: | ---: |
| Popularidad | 0.00% | 0.0000 |
| KNN | 22.63% | 0.1475 |
| MF explícita con fold-in | 0.15% | 0.0005 |
| Híbrido | 21.87% | 0.1363 |

Se seleccionó KNN por NDCG@10. En test: **Recall@10 = 22.65%**, NDCG@10 = **0.1592**.
Recuperó el libro positivo reservado entre los primeros diez para **152 de 671**
usuarios evaluables. No significa “22.65% de precisión” ni que sepamos todos los
libros que cada persona disfrutaría. El resto de libros no calificados no son
necesariamente negativos.

La MF aquí mantiene tres épocas y los parámetros iniciales del notebook. Además,
para una sesión nueva estimamos su perfil a partir de semillas, en vez de usar el
vector de un usuario Goodbooks ya aprendido. Por eso su resultado no contradice
el RMSE del notebook y no demuestra que toda factorización sea inferior. Ajustar
épocas, regularización y fold-in puede cambiar la comparación. La diferencia entre
KNN e híbrido también necesita más particiones o intervalos antes de generalizar.

Los archivos no tienen fechas de interacción: ésta es una evaluación aleatoria
por pares, no una predicción comprobada del futuro. Con historial propio de musa.
conviene hacer evaluación temporal y medir usuarios/libros nuevos por separado.

## 5. Cómo contesta la API

Ejemplo usando la equivalencia real de The Hunger Games:

```http
POST /api/recommendations/books
Authorization: Bearer <access token de musa.>
Content-Type: application/json

{"seeds":["OL5735363W"],"exclude":[],"limit":5}
```

El recorrido es:

```text
App -> Gateway :8084 -> recomendador :8085
                       1. Verifica el JWT de musa.
                       2. Convierte las obras semilla a IDs internos Goodbooks.
                       3. Usa el modelo ya cargado para ordenar libros.
                       4. Excluye semillas, vistos y todas sus equivalencias.
                       5. Devuelve únicamente obras OpenLibrary verificadas.
App -> GET /books/{externalId} -> detalle desde el servicio Java actual
```

Con el modelo real guardado, la prueba devolvió Catching Fire (`OL5735360W`) como
primer resultado de esa semilla. La respuesta también informa la versión del
modelo, que hay 16 libros enlazados de 10,000 y qué semillas no pudieron convertirse.
Los scores ordenan preferencias; no son probabilidades de que te guste un libro.

**Límite actual:** esa lista está restringida a las 16 obras enlazadas. Las métricas
anteriores se calcularon con 10,000 candidatos Goodbooks, no con este subconjunto.
No debemos trasladar esas métricas a la API hasta completar el mapeo y evaluar ese
mismo contrato. El código es integrable, pero la cobertura bibliográfica todavía
es parcial y se muestra en la respuesta.

La API no recibe un usuario Goodbooks elegido por el cliente. Verifica el UUID
del JWT, pero personaliza por las semillas de la sesión. Para usar historial real,
frontend/BFF debe obtener los favoritos/reseñas propios con su token y enviar
positivos como semillas y todos los vistos como exclusiones. Ese consumidor y los
cambios de frontend no se implementaron aquí.

Sin semillas conocidas, devuelve popularidad e informa `unmapped_seeds`. Si no hay
ninguna obra enlazada, falla con 503; no fabrica IDs. Si la equivalencia es ambigua,
ese libro se queda fuera hasta revisarlo. La búsqueda normal de musa. sigue siendo
la fuente para explorar libros ajenos al catálogo entrenado.

## 6. Qué se guarda y cómo se actualiza

Entrenar produce una carpeta de versión nueva con metadatos, vecinos, matrices y
un manifest. El manifest contiene hashes de los CSV y código, parámetros, versiones
de librerías, métricas y cobertura. La API sólo carga esa carpeta y verifica sus
hashes; no entrena al recibir peticiones.

Para mejorar o actualizar:

1. Completar equivalencias con dump/revisión, sin inventar ISBN ni IDs.
2. Incorporar el catálogo e historial autorizado de musa., conservando sus IDs canónicos.
3. Reentrenar offline y comparar métricas con el mismo protocolo.
4. Revisar calidad/cobertura y cambiar `MODEL_PATH` a la nueva versión al desplegar.
5. Para volver atrás, apuntar a una versión anterior; los artefactos no se sobrescriben.

La siguiente etapa musical reutiliza la separación train/inferencia y la API,
pero necesita sus datos e identificadores Spotify. No se mezcla con ratings de
libros ni se simulan reproducciones ausentes.

El README contiene los comandos completos para instalar, auditar, mapear,
entrenar, ejecutar pruebas y levantar el servicio. Los CSV originales y el
notebook no se modificaron.
