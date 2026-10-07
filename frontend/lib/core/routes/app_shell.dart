import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../features/books/presentation/pages/books_home_page.dart';
import '../../features/music/presentation/pages/music_home_page.dart';
import '../../shared/widgets/app_sidebar.dart';

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
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  AppSection _selectedSection = AppSection.books;

  void _changeSection(AppSection section) {
    setState(() {
      _selectedSection = section;
    });
  }

  Widget _buildCurrentPage() {
    switch (_selectedSection) {
      case AppSection.music:
        return const MusicHomePage(
          userId: '550e8400-e29b-41d4-a716-446655440000',
        );

      case AppSection.books:
        return const BooksHomePage();

      case AppSection.home:
        return _placeholderPage(
          'Inicio',
          Icons.home_outlined,
        );

      case AppSection.explore:
        return _placeholderPage(
          'Explorar',
          Icons.explore_outlined,
        );

      case AppSection.social:
        return _placeholderPage(
          'Social',
          Icons.people_outline,
        );

      case AppSection.communities:
        return _placeholderPage(
          'Comunidades',
          Icons.forum_outlined,
        );

      case AppSection.statistics:
        return _placeholderPage(
          'Estadísticas',
          Icons.bar_chart_outlined,
        );

      case AppSection.profile:
        return _placeholderPage(
          'Mi perfil',
          Icons.person_outline,
        );
    }
  }

  Widget _placeholderPage(
    String title,
    IconData icon,
  ) {
    return Container(
      color: AppColors.cream,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 56,
              color: AppColors.ink,
            ),
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
          AppSidebar(
            selectedSection: _selectedSection,
            onSectionSelected: _changeSection,
          ),
          Expanded(
            child: _buildCurrentPage(),
          ),
        ],
      ),
    );
  }
}