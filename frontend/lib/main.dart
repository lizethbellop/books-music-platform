import 'package:flutter/material.dart';

import 'core/routes/app_routes.dart';
import 'core/theme/app_theme.dart';
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
      initialRoute: AppRoutes.home,
      routes: {
        AppRoutes.home: (context) {
          return const HomePage();
        },
        AppRoutes.music: (context) {
          return const MusicHomePage();
        },
        AppRoutes.profile: (context) {
          return const ProfilePage(
            userId: developmentUserId,
          );
        },
      },
    );
  }
}