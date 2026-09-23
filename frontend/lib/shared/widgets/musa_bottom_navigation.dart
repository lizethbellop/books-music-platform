import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../models/musa_destination.dart';

class MusaBottomNavigation extends StatelessWidget {
  final MusaDestination selectedDestination;
  final ValueChanged<MusaDestination> onDestinationSelected;

  const MusaBottomNavigation({
    super.key,
    required this.selectedDestination,
    required this.onDestinationSelected,
  });

  static const destinations = MusaDestination.values;

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: destinations.indexOf(
        selectedDestination,
      ),
      onDestinationSelected: (index) {
        onDestinationSelected(destinations[index]);
      },
      backgroundColor: AppColors.warmWhite,
      indicatorColor: AppColors.lilac,
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.home_outlined),
          selectedIcon: Icon(Icons.home),
          label: 'Inicio',
        ),
        NavigationDestination(
          icon: Icon(Icons.explore_outlined),
          selectedIcon: Icon(Icons.explore),
          label: 'Explorar',
        ),
        NavigationDestination(
          icon: Icon(Icons.music_note_outlined),
          selectedIcon: Icon(Icons.music_note),
          label: 'Música',
        ),
        NavigationDestination(
          icon: Icon(Icons.menu_book_outlined),
          selectedIcon: Icon(Icons.menu_book),
          label: 'Libros',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          selectedIcon: Icon(Icons.person),
          label: 'Perfil',
        ),
      ],
    );
  }
}