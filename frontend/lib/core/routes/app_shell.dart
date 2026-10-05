import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../features/books/presentation/pages/books_home_page.dart';
import '../../features/music/presentation/pages/music_home_page.dart';
import '../../features/profile/presentation/pages/profile_page.dart';
import '../../shared/widgets/musa_sidebar.dart';

enum AppSection {
  home,
  explore,
  music,
  books,
  social,
  communities,
  statistics,
  profile,
}

class AppShell extends StatefulWidget {
  static const defaultUserId = String.fromEnvironment(
    'MUSA_USER_ID',
    defaultValue: '550e8400-e29b-41d4-a716-446655440000',
  );

  final String userId;

  const AppShell({super.key, this.userId = defaultUserId});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  AppSection _selectedSection = AppSection.home;

  void _changeSection(AppSection section) {
    setState(() {
      _selectedSection = section;
    });
  }

  Widget _buildCurrentPage() {
    switch (_selectedSection) {
      case AppSection.music:
        return const MusicHomePage();

      case AppSection.books:
        return const BooksHomePage();

      case AppSection.home:
        return _homeMessage();

      case AppSection.explore:
        return _placeholderPage('Explorar', Icons.explore_outlined);

      case AppSection.social:
        return _placeholderPage('Social', Icons.people_outline);

      case AppSection.communities:
        return _placeholderPage('Comunidades', Icons.forum_outlined);

      case AppSection.statistics:
        return _placeholderPage('Estadísticas', Icons.bar_chart_outlined);

      case AppSection.profile:
        return ProfilePage(userId: widget.userId, embedded: true);
    }
  }

  Widget _homeMessage() {
    return const ColoredBox(
      color: AppColors.cream,
      child: Center(
        child: Text(
          'Bienvenida a Musa.',
          style: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w600,
            color: AppColors.ink,
          ),
        ),
      ),
    );
  }

  Widget _placeholderPage(String title, IconData icon) {
    return Container(
      color: AppColors.cream,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: AppColors.ink),
            const SizedBox(height: 16),
            Text(
              title,
              style: const TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.w600,
                color: AppColors.ink,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          MusaSidebar(
            selectedSection: _selectedSection,
            onSectionSelected: _changeSection,
          ),
          Expanded(child: _buildCurrentPage()),
        ],
      ),
    );
  }
}
