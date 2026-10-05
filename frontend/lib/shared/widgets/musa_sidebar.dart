import 'package:flutter/material.dart';

import '../../core/routes/app_shell.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_text_styles.dart';
import '../models/musa_destination.dart';
import 'musa_profile_avatar.dart';

class MusaSidebar extends StatelessWidget {
  final AppSection selectedSection;
  final ValueChanged<AppSection> onSectionSelected;

  const MusaSidebar({
    super.key,
    required this.selectedSection,
    required this.onSectionSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 230,
      decoration: const BoxDecoration(
        color: AppColors.warmWhite,
        border: Border(right: BorderSide(color: AppColors.border)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 28),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'musa.',
            style: AppTextStyles.logo,
          ),

          const SizedBox(height: 38),

          _SidebarItem(
            icon: Icons.home_outlined,
            title: 'Inicio',
            selected:
                selectedSection == AppSection.home,
            onTap: () {
              onSectionSelected(AppSection.home);
            },
          ),

          _SidebarItem(
            icon: Icons.explore_outlined,
            title: 'Explorar',
            selected:
                selectedSection == AppSection.explore,
            onTap: () {
              onSectionSelected(AppSection.explore);
            },
          ),

          _SidebarItem(
            icon: Icons.music_note_outlined,
            title: 'Música',
            selected:
                selectedSection == AppSection.music,
            onTap: () {
              onSectionSelected(AppSection.music);
            },
          ),

          _SidebarItem(
            icon: Icons.menu_book_outlined,
            title: 'Libros',
            selected:
                selectedSection == AppSection.books,
            onTap: () {
              onSectionSelected(AppSection.books);
            },
          ),

          _SidebarItem(
            icon: Icons.people_outline,
            title: 'Social',
            selected:
                selectedSection == AppSection.social,
            onTap: () {
              onSectionSelected(AppSection.social);
            },
          ),

          _SidebarItem(
            icon: Icons.forum_outlined,
            title: 'Comunidades',
            selected:
                selectedSection ==
                    AppSection.communities,
            onTap: () {
              onSectionSelected(
                AppSection.communities,
              );
            },
          ),

          _SidebarItem(
            icon: Icons.bar_chart_outlined,
            title: 'Estadísticas',
            selected:
                selectedSection ==
                    AppSection.statistics,
            onTap: () {
              onSectionSelected(
                AppSection.statistics,
              );
            },
          ),

          const Spacer(),

          const Divider(color: AppColors.border),

          const SizedBox(height: 12),

          _SidebarItem(
            icon: Icons.person_outline,
            title: 'Mi perfil',
            selected:
                selectedSection == AppSection.profile,
            onTap: () {
              onSectionSelected(AppSection.profile);
            },
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
    final foreground =
        selected
            ? AppColors.ink
            : AppColors.warmWhite;

    return Container(
      margin: const EdgeInsets.only(bottom: 7),
      decoration: BoxDecoration(
        color:
            selected
                ? AppColors.lavender
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
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      ),
    );
  }
}