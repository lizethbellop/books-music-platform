# Servicio de Recomendaciones — musa.

Servicio encargado de generar recomendaciones para la plataforma **musa.**

El servicio se desarrolla en Python y forma parte de la arquitectura distribuida
de la plataforma. Se mantiene independiente de los demás servicios y no accede
directamente a sus bases de datos.

## Casos de uso

El servicio contempla los siguientes casos de uso:

- CU-60: Recomendaciones de libros.
- CU-61: Recomendaciones de canciones.
- CU-62: Recomendaciones de artistas.
- CU-63: Recomendaciones de usuarios.
- CU-64: Recomendaciones de comunidades.

La implementación actual comienza con **CU-60 — Recomendaciones de libros**.

## CU-60 — Recomendaciones de libros

Para el desarrollo inicial del recomendador de libros se utiliza el dataset
Goodbooks-10k como fuente de datos para entrenamiento y experimentación.

El sistema contempla dos enfoques de recomendación:

- **Collaborative Filtering:** utiliza las interacciones y calificaciones de los
  usuarios para identificar patrones de preferencias.
- **Content-Based Filtering:** utiliza características de los libros para
  identificar contenido similar a los gustos del usuario.

Estos enfoques podrán combinarse posteriormente en un sistema híbrido.

### Collaborative Filtering

La primera implementación utiliza Collaborative Filtering basado en
**Matrix Factorization**.

Los datos principales utilizados son:

- `user_id`: identificador del usuario.
- `book_id`: identificador del libro.
- `rating`: calificación otorgada por el usuario.

El objetivo es aprender representaciones latentes de usuarios y libros a partir
de las calificaciones conocidas para posteriormente estimar preferencias sobre
libros no calificados.

## Preparación de datos

El dataset original de ratings contiene:

- 981,756 interacciones.
- 53,424 usuarios.
- 10,000 libros.
- Calificaciones en un rango de 1 a 5.

Durante la exploración se detectaron interacciones repetidas para una misma
combinación de usuario y libro.

Se identificaron 633 combinaciones usuario-libro que contenían calificaciones
diferentes. Debido a que el dataset no contiene información temporal que permita
determinar cuál calificación es la más reciente, las interacciones repetidas se
consolidaron calculando el promedio de sus calificaciones.

Después del procesamiento:

- Interacciones originales: 981,756.
- Interacciones procesadas: 979,478.
- Combinaciones usuario-libro duplicadas: 0.
- Rango de calificación resultante: 1.0 a 5.0.

El dataset original se conserva sin modificaciones y el procesamiento se realiza
sobre una copia denominada `ratings_clean`.

## Estructura del servicio

```text
recommendations-service/
├── app/            # Lógica del servicio
├── data/
│   ├── raw/        # Datasets originales (no versionados)
│   └── processed/  # Datos procesados (no versionados)
├── models/         # Artefactos de modelos entrenados
├── notebooks/      # Exploración y experimentación
├── training/       # Código de entrenamiento
├── tests/          # Pruebas
└── README.md