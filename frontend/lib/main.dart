import 'package:flutter/material.dart';

import 'core/routes/app_routes.dart';
import 'core/routes/app_shell.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/register_screen.dart';
import 'features/auth/presentation/screens/forgot_password_screen.dart';
import 'features/auth/presentation/screens/verify_token_screen.dart';
import 'features/auth/presentation/screens/reset_password_screen.dart';

import 'features/home/presentation/pages/home_page.dart';
import 'features/music/presentation/pages/music_home_page.dart';
import 'features/profile/presentation/pages/profile_page.dart';

void main() {
  runApp(const MusaApp());
}

class MusaApp extends StatelessWidget {
  const MusaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'musa.',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: AppRoutes.login,
      routes: {
        AppRoutes.login: (context) => const LoginScreen(),
        AppRoutes.register: (context) => const RegisterScreen(),
        AppRoutes.forgotPassword: (context) => const ForgotPasswordScreen(),
        AppRoutes.verifyToken: (context) => const VerifyTokenScreen(),
        AppRoutes.resetPassword: (context) => const ResetPasswordScreen(),
        // AppRoutes.home: (context) => const HomeScreen(),
        AppRoutes.home: (context) {
          return const HomePage(userId: AppShell.defaultUserId);
        },
        AppRoutes.music: (context) {
          return const MusicHomePage();
        },
        AppRoutes.profile: (context) {
      return const ProfilePage(userId: AppShell.defaultUserId);
        },
      },
    );
  }
}
