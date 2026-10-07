import 'package:flutter/material.dart';

import 'core/routes/app_routes.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/screens/forgot_password_screen.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/register_screen.dart';
import 'features/auth/presentation/screens/reset_password_screen.dart';
import 'features/auth/presentation/screens/verify_token_screen.dart';
import 'features/home/presentation/pages/home_page.dart';
import 'features/music/presentation/pages/music_home_page.dart';
import 'features/profile/presentation/pages/profile_page.dart';

// ID temporal de pruebas para desarrollo local
const String developmentUserId = '1';

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
        AppRoutes.home: (context) => const HomePage(userId: developmentUserId),
        //AppRoutes.music: (context) => const MusicHomePage(userId: developmentUserId),
        AppRoutes.profile: (context) => const ProfilePage(userId: developmentUserId),
      },
    );
  }
}