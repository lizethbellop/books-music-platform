import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_text_styles.dart';
import '../models/musa_destination.dart';

class MusaSidebar extends StatelessWidget {
  final MusaDestination selectedDestination;
  final ValueChanged<MusaDestination>? onDestinationSelected;

  const MusaSidebar({
    super.key,
    this.selectedDestination = MusaDestination.music,
    this.onDestinationSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 230,
      decoration: const BoxDecoration(
        color: AppColors.warmWhite,
        border: Border(
          right: BorderSide(
            color: AppColors.border,
          ),
        ),
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: 18,
        vertical: 28,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _MusaLogo(),

          const SizedBox(height: 38),

          _SidebarItem(
            icon: Icons.home_outlined,
            title: 'Inicio',
            selected:
                selectedDestination == MusaDestination.home,
            onTap: () => _select(MusaDestination.home),
          ),

          _SidebarItem(
            icon: Icons.explore_outlined,
            title: 'Explorar',
            selected:
                selectedDestination == MusaDestination.explore,
            onTap: () => _select(MusaDestination.explore),
          ),

          _SidebarItem(
            icon: Icons.music_note_outlined,
            title: 'Música',
            selected:
                selectedDestination == MusaDestination.music,
            onTap: () => _select(MusaDestination.music),
          ),

          _SidebarItem(
            icon: Icons.menu_book_outlined,
            title: 'Libros',
            selected:
                selectedDestination == MusaDestination.books,
            onTap: () => _select(MusaDestination.books),
          ),

          const Spacer(),

          const Divider(color: AppColors.border),

          const SizedBox(height: 12),

          _SidebarItem(
            icon: Icons.person_outline,
            title: 'Perfil',
            selected:
                selectedDestination == MusaDestination.profile,
            onTap: () => _select(MusaDestination.profile),
          ),
        ],
      ),
    );
  }

  void _select(MusaDestination destination) {
    onDestinationSelected?.call(destination);
  }
}

class _MusaLogo extends StatelessWidget {
  const _MusaLogo();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 150,
      height: 55,
      child: ClipRect(
        child: Image.asset(
          'assets/images/musa_logo.png',
          fit: BoxFit.cover,
          alignment: Alignment.center,
        ),
      ),
    );
  }
}

class _SidebarItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool selected;
  final VoidCallback onTap;

  const _SidebarItem({
    required this.icon,
    required this.title,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 7),
      decoration: BoxDecoration(
        color: selected
            ? AppColors.lilac
            : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        dense: true,
        onTap: onTap,
        leading: Icon(
          icon,
          color: AppColors.ink,
        ),
        title: Text(
          title,
          style: AppTextStyles.navigation.copyWith(
            color: AppColors.ink,
            fontWeight: selected
                ? FontWeight.w700
                : FontWeight.w500,
          ),
        ),
      ),
    );
  }
}