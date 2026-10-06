import 'package:flutter/material.dart';

import 'core/routes/app_routes.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/auth/presentation/screens/register_screen.dart';
import 'features/home/presentation/pages/home_page.dart';
import 'features/music/presentation/pages/music_home_page.dart';
import 'features/profile/presentation/pages/profile_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static const String developmentUserId =
      '550e8400-e29b-41d4-a716-446655440000';

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Musa',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: AppRoutes.login,
      routes: {
        AppRoutes.login: (context) => const LoginScreen(),
        AppRoutes.register: (context) => const RegisterScreen(),
        // AppRoutes.home: (context) => const HomeScreen(),
        AppRoutes.home: (context) {
          return const HomePage(userId: developmentUserId);
        },
        AppRoutes.music: (context) {
          return const MusicHomePage(userId: developmentUserId);
        },
        AppRoutes.profile: (context) {
          return const ProfilePage(userId: developmentUserId);
        },
      },
    );
  }
}
