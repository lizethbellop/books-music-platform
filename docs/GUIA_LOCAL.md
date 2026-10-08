# Musa: qué configurar y qué probar

Empieza por la guía de tu computadora: [Windows](GUIA_WINDOWS.md) o [Mac](GUIA_MAC.md). Ambas siguen el mismo orden: PostgreSQL → Redis → cuatro .env → cuatro backend → Flutter → recorrido manual.

## Qué hace cada cosa

| Programa | Para qué sirve | Puerto |
|---|---|---|
| PostgreSQL | Guarda datos en cuatro bases independientes | 5432 |
| Redis | Guarda las sesiones de autenticación y permite renovar el acceso | 6379 |
| Usuarios | Registro, login, username y tokens | 8083 |
| Perfil | Perfil, preferencias y listas | 8082 |
| Libros | Catálogo, biblioteca y reseñas de libros | 8081 |
| Música | Catálogo, favoritos y reseñas musicales | 8080 |
| Flutter | Interfaz que abres para probar | Web: 3000 |

En Windows, esta guía ejecuta Redis con comandos de Docker; requiere Docker instalado y funcionando, sin exigir una aplicación específica. En la Mac de Liz, Redis está instalado directamente. Los cuatro backend se ejecutan con Java, PostgreSQL se ejecuta instalado y Flutter se ejecuta con Flutter.

## Qué escribir en los .env

Cada servicio tiene su propio archivo .env dentro de backend/NOMBRE-service. Copia su .env.example para crearlo **solo si no existe**. Si ya funciona, conserva sus valores. En Windows revisa que no se llame .env.txt.

Escribe CLAVE=valor, sin comillas ni espacios alrededor de =. Sustituye todos los REEMPLAZAR con tus datos reales. Esos marcadores son explicaciones, no credenciales que funcionen.

**Contraseñas de BD:** son las que asignaste a auth_user, profile_user, books_user y music_user en PostgreSQL. Si tu instalación anterior usa postgres u otro propietario, conserva ese usuario y su contraseña; no cambies una instalación funcional para hacerla coincidir con el ejemplo.

**JWT_SECRET:** es una clave privada con la que Usuarios firma los tokens y los otros servicios los verifican. No es el token que recibes al iniciar sesión, ni una API key de Spotify. En una instalación nueva genera una clave y copia el mismo valor en los cuatro .env. Cada integrante puede tener una clave distinta en su computadora. No regenerarla en cada arranque: cambiarla invalida los tokens existentes y exige volver a iniciar sesión.

El script setup-local-env.py reutiliza la clave local existente o genera una nueva y la coloca en los .env nuevos o con JWT_SECRET vacío. Conserva las demás credenciales. Si detecta claves distintas entre servicios, se detiene sin modificar archivos para que puedas unificarlas. El repositorio actual publica las plantillas con la clave vacía, no tu clave privada.

### Usuarios — backend/users-service/.env

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/BDAuth
DB_USERNAME=auth_user
DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_AUTH_USER
JWT_SECRET=REEMPLAZAR_CON_LA_CLAVE_GENERADA
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
MAX_FAILED_ATTEMPTS=3
LOCK_TIME_MINUTES=5
MAIL_USERNAME=REEMPLAZAR_CON_TU_CORREO
MAIL_PASSWORD=REEMPLAZAR_CON_LA_CONTRASENA_DE_APLICACION_DEL_CORREO
USERS_SERVICE_PORT=8083
```

El correo de bienvenida forma parte del registro actual; configurar SMTP correctamente para probarlo. MAIL_PASSWORD es la contraseña de aplicación del proveedor, no la contraseña normal. Los parámetros actuales usan Gmail. No inventar valores ni dejar los marcadores REEMPLAZAR.

### Perfil — backend/profile-service/.env

```dotenv
PROFILE_DB_URL=jdbc:postgresql://localhost:5432/profile_db
PROFILE_DB_USERNAME=profile_user
PROFILE_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_PROFILE_USER
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
CLOUDINARY_URL=REEMPLAZAR_CON_TU_URL_DE_CLOUDINARY
PROFILE_SERVICE_PORT=8082
BOOKS_SERVICE_URL=http://localhost:8081
MUSIC_SERVICE_URL=http://localhost:8080
```

Cloudinary se necesita para subir fotos de perfil. Usar la credencial privada del dashboard local y no compartirla en Git.

### Libros — backend/books-service/.env

```dotenv
BOOKS_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_BOOKS_USER
DEEPL_API_KEY=REEMPLAZAR_CON_TU_CLAVE_DE_DEEPL
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
BOOKS_SERVICE_PORT=8081
```

La configuración actual usa books_db y books_user. Open Library no requiere API key; DeepL utiliza una clave propia del proveedor.

### Música — backend/music-service/.env

```dotenv
MUSIC_DB_PASSWORD=REEMPLAZAR_CON_LA_CLAVE_DE_MUSIC_USER
SPOTIFY_CLIENT_ID=REEMPLAZAR_CON_TU_CLIENT_ID
SPOTIFY_CLIENT_SECRET=REEMPLAZAR_CON_TU_CLIENT_SECRET
JWT_SECRET=REEMPLAZAR_CON_LA_MISMA_CLAVE_GENERADA
MUSIC_SERVICE_PORT=8080
```

La configuración actual usa music_db y music_user. Spotify necesita las credenciales de una aplicación del proveedor. Los backend leen el .env como archivo de propiedades; no hace falta cargarlo con comandos Bash ni instalar dotenv.


## Comprobar el flujo, como lo hicimos en local

Primero espera a que los cuatro backend indiquen Started. Luego abre Flutter:

1. Registra una cuenta con nombre completo, username, correo y contraseña. Usa un username nuevo; la unicidad no distingue mayúsculas.
2. Inicia sesión por correo. Abre Perfil y comprueba que se muestre el username y el avatar.
3. Agrega preferencias de libro, canción y artista. Revisa que las portadas se vean completas y que puedas eliminar elementos.
4. Crea una lista, busca contenido y agrégalo. Cierra y vuelve a abrir la lista: deben conservarse los elementos. Prueba eliminar y agregar un duplicado.
5. Busca un libro: prueba estado de lectura, favorito, calificación y crear/editar/eliminar reseña.
6. Busca música: prueba favorito, calificación y crear/editar/eliminar reseña. Debe aparecer un solo sidebar claro.
7. Entra con una segunda cuenta y consulta las reseñas: deben mostrar el username de la autora.
8. Cierra sesión y comprueba que regrese al login.
9. Sin Recordarme, cierra completamente la aplicación y ábrela otra vez: debe pedir login. Con Recordarme debe conservar la sesión, hasta 7 días desde el login. En web usa siempre localhost:3000. El acceso dura 15 minutos y se renueva con el refresh token mientras la sesión siga vigente.

Estas guías son para probar en la misma computadora. Para un teléfono físico hay que configurar direcciones de red aparte.

## Si algo falla

| Qué ves | Qué revisar |
|---|---|
| Connection refused en 5432 | PostgreSQL no está encendido |
| Connection refused en 6379 | Redis no está encendido; comprobar PONG |
| password authentication failed | Usuario/contraseña de BD del .env |
| relation does not exist / schema validation | Tablas o migración pendientes |
| 401 en Libros/Música/Perfil | Misma JWT_SECRET en los cuatro servicios; reiniciar backend e iniciar sesión otra vez |
| Falla al registrarse por correo | Credenciales SMTP de Usuarios y su terminal |
| Falla una API externa | Spotify/DeepL/Cloudinary del servicio correspondiente |
| Port already in use | Ya hay otra copia corriendo; detenerla con Ctrl+C |
| Interfaz vieja o doble sidebar | Actualizar Git, detener Flutter y volver a ejecutar; recarga completa del navegador |

Los .env no se suben. Desde la raíz, git check-ignore debe listar los cuatro:

```text
git check-ignore backend/users-service/.env backend/profile-service/.env backend/books-service/.env backend/music-service/.env
```

Para las pruebas automáticas y sus requisitos, consulta [Inventario](CONTEO_PRUEBAS.md) y [Configuración técnica](CONFIGURACION_LOCAL.md). El recorrido manual anterior sigue siendo necesario.
