import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../models/musa_destination.dart';
import 'musa_profile_avatar.dart';

class MusaBottomNavigation extends StatelessWidget {
  final MusaDestination selectedDestination;
  final ValueChanged<MusaDestination> onDestinationSelected;
  final String? profilePictureUrl;

  const MusaBottomNavigation({
    super.key,
    required this.selectedDestination,
    required this.onDestinationSelected,
    this.profilePictureUrl,
  });

  static const destinations = MusaDestination.values;

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: destinations.indexOf(selectedDestination),
      onDestinationSelected: (index) {
        onDestinationSelected(destinations[index]);
      },
      backgroundColor: AppColors.warmWhite,
      indicatorColor: AppColors.lilac,
      destinations: [
        const NavigationDestination(
          icon: Icon(Icons.home_outlined),
          selectedIcon: Icon(Icons.home),
          label: 'Inicio',
        ),
        const NavigationDestination(
          icon: Icon(Icons.explore_outlined),
          selectedIcon: Icon(Icons.explore),
          label: 'Explorar',
        ),
        const NavigationDestination(
          icon: Icon(Icons.music_note_outlined),
          selectedIcon: Icon(Icons.music_note),
          label: 'Música',
        ),
        const NavigationDestination(
          icon: Icon(Icons.menu_book_outlined),
          selectedIcon: Icon(Icons.menu_book),
          label: 'Libros',
        ),
        NavigationDestination(
          icon: MusaProfileAvatar(imageUrl: profilePictureUrl, radius: 12),
          selectedIcon: MusaProfileAvatar(
            imageUrl: profilePictureUrl,
            radius: 12,
          ),
          label: 'Perfil',
        ),
      ],
    );
  }
}
