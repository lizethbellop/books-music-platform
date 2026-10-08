import 'package:flutter/material.dart';

import 'core/routes/app_routes.dart';
import 'core/routes/app_shell.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/data/services/auth_api_service.dart';
import 'features/auth/data/services/auth_session_manager.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/register_screen.dart';
import 'features/auth/presentation/screens/forgot_password_screen.dart';
import 'features/auth/presentation/screens/verify_token_screen.dart';
import 'features/auth/presentation/screens/reset_password_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  try {
    final manager = AuthSessionManager.instance;
    await manager.restore();

    if (manager.session != null) {
      try {
        await manager.getAccessToken();
      } on AuthApiException catch (error) {
        if (error.statusCode != 401 && error.statusCode != 403) {
          rethrow;
        }
      }
    }

    runApp(const MusaApp());
  } catch (_) {
    runApp(
      MaterialApp(
        theme: AppTheme.light,
        home: Scaffold(
          body: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('No se pudo recuperar la sesión.'),
                const SizedBox(height: 16),
                FilledButton(onPressed: main, child: const Text('Reintentar')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class MusaApp extends StatefulWidget {
  const MusaApp({super.key});

  @override
  State<MusaApp> createState() => _MusaAppState();
}

class _MusaAppState extends State<MusaApp> {
  final _navigatorKey = GlobalKey<NavigatorState>();
  final _manager = AuthSessionManager.instance;
  String? _lastUserId;

  @override
  void initState() {
    super.initState();
    _lastUserId = _manager.session?.userId;
    _manager.addListener(_sessionChanged);
  }

  @override
  void dispose() {
    _manager.removeListener(_sessionChanged);
    super.dispose();
  }

  void _sessionChanged() {
    final previous = _lastUserId;
    _lastUserId = _manager.session?.userId;

    if (previous != null && _lastUserId == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || _manager.session != null) return;
        _navigatorKey.currentState?.pushNamedAndRemoveUntil(
          AppRoutes.login,
          (_) => false,
        );
      });
      setState(() {});
    }
  }

  Widget _authenticatedPage(AppSection section) {
    final session = _manager.session;

    if (session == null) {
      return const LoginScreen();
    }

    return AppShell(userId: session.userId, initialSection: section);
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    navigatorKey: _navigatorKey,
    title: 'musa.',
    debugShowCheckedModeBanner: false,
    theme: AppTheme.light,
    themeMode: ThemeMode.light,
    initialRoute: _manager.session == null ? AppRoutes.login : AppRoutes.home,
    routes: {
      AppRoutes.login: (_) => const LoginScreen(),
      AppRoutes.register: (_) => const RegisterScreen(),
      AppRoutes.forgotPassword: (_) => const ForgotPasswordScreen(),
      AppRoutes.verifyToken: (_) => const VerifyTokenScreen(),
      AppRoutes.resetPassword: (_) => const ResetPasswordScreen(),
      AppRoutes.home: (_) => _authenticatedPage(AppSection.home),
      AppRoutes.music: (_) => _authenticatedPage(AppSection.music),
      AppRoutes.books: (_) => _authenticatedPage(AppSection.books),
      AppRoutes.profile: (_) => _authenticatedPage(AppSection.profile),
    },
  );
}
