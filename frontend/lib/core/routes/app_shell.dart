import 'package:flutter/material.dart';

import 'app_section.dart';
import '../../core/theme/app_colors.dart';
import '../../features/home/presentation/pages/home_page.dart';
import '../../features/profile/presentation/pages/profile_page.dart';
import '../../features/books/presentation/pages/books_home_page.dart';
import '../../features/music/presentation/pages/music_home_page.dart';
import '../../shared/models/musa_destination.dart';
import '../../shared/widgets/musa_navigation_shell.dart';
export 'app_section.dart';

/// The single owner of application navigation on every platform.
class AppShell extends StatefulWidget {
  static const defaultUserId = String.fromEnvironment(
    'MUSA_USER_ID',
    defaultValue: '550e8400-e29b-41d4-a716-446655440000',
  );
  final String userId;
  final AppSection initialSection;
  const AppShell({
    super.key,
    this.userId = defaultUserId,
    this.initialSection = AppSection.home,
  });
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  late AppSection _section = widget.initialSection;
  Key _navigationKey = UniqueKey();
  void _select(AppSection section) => setState(() => _section = section);

  Widget _page() => switch (_section) {
    AppSection.home => HomePage(userId: widget.userId, embedded: true),
    AppSection.music => MusicHomePage(userId: widget.userId),
    AppSection.books => BooksHomePage(userId: widget.userId),
    AppSection.profile => ProfilePage(
      userId: widget.userId,
      embedded: true,
      onProfileUpdated: () => setState(() => _navigationKey = UniqueKey()),
    ),
    _ => Center(
      child: Text(
        '${switch (_section) {
          AppSection.explore => 'Explorar',
          AppSection.social => 'Social',
          AppSection.communities => 'Comunidades',
          _ => 'Estadísticas',
        }} — pendiente',
        style: const TextStyle(color: AppColors.ink),
      ),
    ),
  };

  @override
  Widget build(BuildContext context) => MusaNavigationShell(
    key: _navigationKey,
    userId: widget.userId,
    selectedSection: _section,
    onSectionSelected: _select,
    selectedDestination: switch (_section) {
      AppSection.music => MusaDestination.music,
      AppSection.books => MusaDestination.books,
      AppSection.profile => MusaDestination.profile,
      AppSection.explore => MusaDestination.explore,
      _ => MusaDestination.home,
    },
    onDestinationSelected: (destination) => _select(switch (destination) {
      MusaDestination.home => AppSection.home,
      MusaDestination.explore => AppSection.explore,
      MusaDestination.music => AppSection.music,
      MusaDestination.books => AppSection.books,
      MusaDestination.profile => AppSection.profile,
    }),
    child: _page(),
  );
}
