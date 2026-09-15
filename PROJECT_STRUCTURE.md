# Estructura del proyecto `books-music-platform`

Esta guía define cómo organizar el monorepo y, en particular, el frontend Flutter. Su objetivo es que cualquier integrante del equipo pueda decidir rápidamente dónde colocar una nueva pieza de código.

> Los nombres de servicios y archivos que aparecen aquí son ejemplos de organización. No obligan a crear todos desde el inicio ni a usar una tecnología específica en el backend.

## 1. Estructura general del monorepo

```text
books-music-platform/
├── frontend/                 # Una sola aplicación Flutter
├── backend/                  # Servicios de backend independientes
│   ├── auth-service/         # Ejemplo: autenticación y sesiones
│   ├── books-service/        # Ejemplo: catálogo y datos de libros
│   └── music-service/        # Ejemplo: catálogo y datos de música
├── docs/                     # Documentación compartida del proyecto
├── .gitignore
├── README.md                 # Introducción, instalación y comandos principales
└── PROJECT_STRUCTURE.md      # Esta guía de arquitectura
```

### `frontend/`

Contiene **una sola aplicación Flutter** para las plataformas habilitadas en el proyecto. Todo el código Dart propio de la aplicación vive principalmente en `frontend/lib/`.

Aunque la aplicación tenga módulos de libros, música, autenticación y perfil, estos no deben convertirse en proyectos Flutter separados. Se organizan como funcionalidades dentro de `lib/features/`.

### `backend/`

Contiene servicios independientes. Cada servicio debe poder tener su propia configuración, pruebas, dependencias y proceso de ejecución o despliegue.

Una estructura típica de un servicio podría ser:

```text
backend/auth-service/
├── src/
├── tests/
├── README.md
└── archivo-de-dependencias
```

Reglas recomendadas:

- Un servicio es responsable de un dominio claro, por ejemplo autenticación, libros o música.
- Un servicio no debe importar directamente el código interno de otro servicio.
- La comunicación entre servicios debe hacerse mediante contratos explícitos, por ejemplo una API o eventos.
- Cada servicio debe documentar cómo configurarlo, ejecutarlo y probarlo en su propio `README.md`.
- No es necesario crear un servicio hasta que exista una necesidad real. Se debe evitar generar carpetas vacías “por si acaso”.

### `docs/`

Contiene documentación que afecta a más de una parte del sistema, por ejemplo:

- decisiones de arquitectura;
- contratos de API;
- diagramas y flujos de negocio;
- convenciones de Git y revisiones de código;
- instrucciones de desarrollo local;
- estrategia de despliegue.

La documentación exclusiva de un servicio o módulo debe permanecer cerca de ese código, normalmente en su propio `README.md`.

## 2. Estructura de `frontend/lib`

```text
frontend/lib/
├── main.dart
├── app.dart
├── core/
├── features/
└── shared/
```

### `main.dart`

Es el punto de entrada. Debe mantenerse pequeño: inicializa lo estrictamente necesario y ejecuta la aplicación.

```dart
import 'package:flutter/material.dart';

import 'app.dart';

void main() {
  runApp(const App());
}
```

### `app.dart`

Define el widget raíz y conecta la configuración global, como tema, rutas y navegación.

### `core/`

Contiene infraestructura y configuración transversal que pertenece a la aplicación completa, no a una funcionalidad concreta.

```text
core/
├── config/                   # Configuración de ambientes y constantes globales
├── errors/                   # Errores y excepciones comunes
├── network/                  # Cliente HTTP y configuración de red
├── routing/                  # Rutas y navegación global
├── theme/                    # Tema, colores y tipografía de la aplicación
└── utils/                    # Utilidades realmente globales
```

Ejemplos de elementos correctos para `core/`:

- nombres o definiciones de rutas globales;
- configuración base del cliente de red;
- tema claro y oscuro;
- tipos de error compartidos;
- lectura de configuración por ambiente.

No se debe colocar en `core/` una clase solo porque podría reutilizarse algún día. Si pertenece exclusivamente al inicio de sesión, va en `features/auth/`.

### `features/`

Contiene las funcionalidades del producto. Cada funcionalidad reúne su interfaz, lógica y acceso a datos, evitando repartir su código por toda la aplicación.

```text
features/
├── auth/
├── books/
├── music/
└── profile/
```

Cada funcionalidad puede organizarse en tres capas sencillas:

```text
feature_name/
├── data/                     # Modelos de datos, fuentes y repositorios
├── logic/                    # Estado, controladores y reglas de la funcionalidad
└── presentation/             # Pantallas y widgets
```

Esta separación no exige una clase o interfaz por cada concepto. Se debe empezar con lo mínimo y dividir archivos cuando el código realmente lo necesite.

### `shared/`

Contiene piezas visuales o utilidades pequeñas que ya son usadas por varias funcionalidades.

```text
shared/
├── widgets/                  # Widgets reutilizados por distintas funcionalidades
├── formatters/               # Formateadores compartidos
└── extensions/               # Extensiones de Dart de uso general
```

Ejemplos:

- un indicador de carga usado en autenticación, libros y música;
- un diálogo de error común;
- un widget de imagen con estado de carga;
- un formateador de fechas compartido.

Un widget usado únicamente por la pantalla de login debe quedarse en `features/auth/presentation/widgets/`, no en `shared/widgets/`.

## 3. Regla rápida para elegir una carpeta

Antes de crear un archivo, se puede seguir este orden:

1. Si pertenece a una funcionalidad concreta, va en `features/<funcionalidad>/`.
2. Si configura o soporta toda la aplicación, va en `core/`.
3. Si ya se utiliza desde varias funcionalidades y no pertenece a una sola, va en `shared/`.
4. Si todavía no se reutiliza, debe permanecer junto a la funcionalidad que lo necesita.

## 4. Ejemplo: inicio de sesión en Flutter

El siguiente ejemplo muestra una implementación pequeña y completa en cuanto a organización. No crea una interfaz visual final ni realiza llamadas HTTP reales; sirve como guía para ubicar responsabilidades.

### 4.1 Árbol de archivos

```text
frontend/lib/
├── app.dart
├── core/
│   └── routing/
│       ├── app_routes.dart
│       └── app_router.dart
└── features/
    └── auth/
        ├── data/
        │   ├── models/
        │   │   └── user.dart
        │   └── repositories/
        │       └── auth_repository.dart
        ├── logic/
        │   └── login_controller.dart
        └── presentation/
            ├── screens/
            │   └── login_screen.dart
            └── widgets/
                └── login_form.dart
```

El flujo de dependencias es:

```text
LoginScreen / LoginForm
          │
          ▼
   LoginController
          │
          ▼
   AuthRepository
          │
          ▼
        User
```

La pantalla no debe conocer detalles de red. El repositorio no debe controlar widgets. El controlador coordina ambos lados y expone el estado necesario para la interfaz.

### 4.2 Modelo: `data/models/user.dart`

Representa los datos que la funcionalidad necesita después de autenticar al usuario.

```dart
class User {
  const User({
    required this.id,
    required this.email,
    required this.displayName,
  });

  final String id;
  final String email;
  final String displayName;

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      email: json['email'] as String,
      displayName: json['displayName'] as String,
    );
  }
}
```

El modelo no muestra mensajes, no navega y no contiene widgets.

### 4.3 Acceso a datos: `data/repositories/auth_repository.dart`

Centraliza la operación de autenticación. Cuando exista un backend, este archivo podrá usar el cliente de red configurado en `core/network/`.

```dart
import '../models/user.dart';

class AuthRepository {
  Future<User> login({
    required String email,
    required String password,
  }) async {
    // Ejemplo temporal. Aquí se conectará el servicio real de autenticación.
    await Future<void>.delayed(const Duration(milliseconds: 500));

    if (email.isEmpty || password.isEmpty) {
      throw Exception('Correo y contraseña son obligatorios.');
    }

    return User(
      id: 'example-user-id',
      email: email,
      displayName: 'Usuario de ejemplo',
    );
  }
}
```

Para una primera versión pequeña, una clase concreta es suficiente. Se puede añadir una interfaz o separar una fuente remota cuando haya más de una implementación, pruebas que lo justifiquen o mayor complejidad; no es necesario hacerlo de forma preventiva.

### 4.4 Lógica y estado: `logic/login_controller.dart`

Valida la interacción, ejecuta el repositorio y mantiene el estado que consume la pantalla.

```dart
import 'package:flutter/foundation.dart';

import '../data/models/user.dart';
import '../data/repositories/auth_repository.dart';

class LoginController extends ChangeNotifier {
  LoginController(this._authRepository);

  final AuthRepository _authRepository;

  bool isLoading = false;
  String? errorMessage;
  User? user;

  Future<bool> submit({
    required String email,
    required String password,
  }) async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();

    try {
      user = await _authRepository.login(
        email: email.trim(),
        password: password,
      );
      return true;
    } catch (error) {
      errorMessage = error.toString();
      return false;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
```

`ChangeNotifier` forma parte de Flutter, por lo que este ejemplo no añade una dependencia de manejo de estado. Si el equipo adopta otra solución en el futuro, la responsabilidad de esta capa debe mantenerse: coordinar el caso de uso y exponer su estado.

### 4.5 Widget del formulario: `presentation/widgets/login_form.dart`

Contiene los campos y la acción del formulario. Se mantiene dentro de `auth` porque no es un componente genérico.

```dart
import 'package:flutter/material.dart';

import '../../logic/login_controller.dart';

class LoginForm extends StatefulWidget {
  const LoginForm({
    required this.controller,
    required this.onSuccess,
    super.key,
  });

  final LoginController controller;
  final VoidCallback onSuccess;

  @override
  State<LoginForm> createState() => _LoginFormState();
}

class _LoginFormState extends State<LoginForm> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final success = await widget.controller.submit(
      email: _emailController.text,
      password: _passwordController.text,
    );

    if (success && mounted) {
      widget.onSuccess();
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.controller,
      builder: (context, child) {
        return Column(
          children: [
            TextField(
              controller: _emailController,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Correo'),
            ),
            TextField(
              controller: _passwordController,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Contraseña'),
            ),
            if (widget.controller.errorMessage != null)
              Text(widget.controller.errorMessage!),
            ElevatedButton(
              onPressed: widget.controller.isLoading ? null : _submit,
              child: Text(
                widget.controller.isLoading ? 'Ingresando…' : 'Ingresar',
              ),
            ),
          ],
        );
      },
    );
  }
}
```

Este formulario es funcional como ejemplo, pero no pretende definir el diseño visual final, las reglas completas de validación ni los mensajes definitivos del producto.

### 4.6 Pantalla: `presentation/screens/login_screen.dart`

La pantalla compone la vista y decide qué ocurre después de un inicio de sesión exitoso.

```dart
import 'package:flutter/material.dart';

import '../../../../core/routing/app_routes.dart';
import '../../data/repositories/auth_repository.dart';
import '../../logic/login_controller.dart';
import '../widgets/login_form.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  late final LoginController _controller;

  @override
  void initState() {
    super.initState();
    _controller = LoginController(AuthRepository());
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Iniciar sesión')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: LoginForm(
          controller: _controller,
          onSuccess: () {
            Navigator.of(context).pushReplacementNamed(AppRoutes.home);
          },
        ),
      ),
    );
  }
}
```

En una aplicación más grande, la creación de `AuthRepository` y `LoginController` puede moverse a un punto central de composición o inyección. Para este ejemplo se mantiene explícita y sin dependencias adicionales.

### 4.7 Nombres de rutas: `core/routing/app_routes.dart`

```dart
abstract final class AppRoutes {
  static const login = '/login';
  static const home = '/home';
}
```

### 4.8 Configuración de rutas: `core/routing/app_router.dart`

```dart
import 'package:flutter/material.dart';

import '../../features/auth/presentation/screens/login_screen.dart';
import 'app_routes.dart';

abstract final class AppRouter {
  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case AppRoutes.login:
        return MaterialPageRoute<void>(
          builder: (_) => const LoginScreen(),
        );
      case AppRoutes.home:
        return MaterialPageRoute<void>(
          builder: (_) => const Scaffold(
            body: Center(child: Text('Inicio')),
          ),
        );
      default:
        return MaterialPageRoute<void>(
          builder: (_) => const LoginScreen(),
        );
    }
  }
}
```

La pantalla de inicio usada aquí es solo un destino mínimo para explicar la navegación. Cuando se implemente la funcionalidad real, debe vivir en su propio módulo, por ejemplo `features/home/presentation/screens/home_screen.dart`.

### 4.9 Conexión en `app.dart`

```dart
import 'package:flutter/material.dart';

import 'core/routing/app_router.dart';
import 'core/routing/app_routes.dart';

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Books & Music',
      initialRoute: AppRoutes.login,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}
```

Centralizar los nombres en `AppRoutes` evita cadenas duplicadas y errores de escritura entre pantallas.

## 5. Cómo extender el ejemplo cuando llegue el backend

Cuando exista el servicio real de autenticación, el cambio principal estará en la capa de datos:

1. Configurar el cliente de red común en `core/network/`.
2. Hacer que `AuthRepository.login` envíe las credenciales al servicio.
3. Convertir la respuesta a `User`.
4. Traducir errores técnicos a errores comprensibles para la lógica y la interfaz.
5. Guardar el token o la sesión mediante un componente adecuado, sin exponerlos en widgets.

`LoginForm` no debería saber qué URL se usa, cómo se serializa la petición ni dónde se guarda el token. Esa separación permite modificar la API sin rehacer la pantalla.

## 6. Pruebas recomendadas

Las pruebas deben reflejar la ubicación del código dentro de `lib/`:

```text
frontend/test/
└── features/
    └── auth/
        ├── data/
        │   └── repositories/
        │       └── auth_repository_test.dart
        ├── logic/
        │   └── login_controller_test.dart
        └── presentation/
            └── login_screen_test.dart
```

- Probar el repositorio para confirmar cómo procesa respuestas y errores.
- Probar el controlador para confirmar sus cambios de carga, éxito y error.
- Probar la pantalla o el formulario para confirmar la interacción visible.

No es obligatorio crear todos estos archivos antes de implementar la funcionalidad correspondiente.

## 7. Convenciones para nuevas funcionalidades

Al agregar una funcionalidad como libros favoritos:

1. Crear `features/favorites/`.
2. Añadir únicamente las capas y carpetas necesarias.
3. Mantener sus widgets específicos dentro de `features/favorites/presentation/widgets/`.
4. Mover una pieza a `shared/` solo después de que varias funcionalidades la usen.
5. Colocar configuración verdaderamente global en `core/`.
6. Añadir o registrar sus rutas en `core/routing/`.
7. Crear pruebas siguiendo la misma estructura bajo `test/features/favorites/`.

La meta no es producir la mayor cantidad de capas posible, sino mantener juntas las piezas de una funcionalidad, separar responsabilidades y permitir que el proyecto crezca sin perder claridad.
