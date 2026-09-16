import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_text_styles.dart';

class MusaSidebar extends StatelessWidget {
  const MusaSidebar({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 230,
      color: AppColors.ink,
      padding: const EdgeInsets.symmetric(
        horizontal: 18,
        vertical: 28,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Musa',
            style: AppTextStyles.logo,
          ),

          const SizedBox(height: 38),

          const _SidebarItem(
            icon: Icons.home_outlined,
            title: 'Inicio',
          ),

          const _SidebarItem(
            icon: Icons.explore_outlined,
            title: 'Explorar',
          ),

          const _SidebarItem(
            icon: Icons.music_note_outlined,
            title: 'Música',
            selected: true,
          ),

          const _SidebarItem(
            icon: Icons.menu_book_outlined,
            title: 'Libros',
          ),

          const _SidebarItem(
            icon: Icons.people_outline,
            title: 'Social',
          ),

          const _SidebarItem(
            icon: Icons.forum_outlined,
            title: 'Comunidades',
          ),

          const _SidebarItem(
            icon: Icons.bar_chart_outlined,
            title: 'Estadísticas',
          ),

          const Spacer(),

          const Divider(
            color: Colors.white24,
          ),

          const SizedBox(height: 12),

          Row(
            children: [
              const CircleAvatar(
                radius: 18,
                backgroundColor: AppColors.lavender,
                child: Icon(
                  Icons.person,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(width: 12),
              Text(
                'Mi perfil',
                style: AppTextStyles.navigation,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SidebarItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool selected;

  const _SidebarItem({
    required this.icon,
    required this.title,
    this.selected = false,
  });

  @override
  Widget build(BuildContext context) {
    final foreground =
        selected ? AppColors.ink : AppColors.warmWhite;

    return Container(
      margin: const EdgeInsets.only(bottom: 7),
      decoration: BoxDecoration(
        color: selected
            ? AppColors.lavender
            : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        dense: true,
        leading: Icon(
          icon,
          color: foreground,
        ),
        title: Text(
          title,
          style: AppTextStyles.navigation.copyWith(
            color: foreground,
          ),
        ),
      ),
    );
  }
}
