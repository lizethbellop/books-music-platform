import 'package:flutter/material.dart';

import '../../core/routes/app_section.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_text_styles.dart';
import 'musa_profile_avatar.dart';

class MusaSidebar extends StatelessWidget {
  final AppSection selectedSection;
  final ValueChanged<AppSection> onSectionSelected;
  final String? profilePictureUrl;

  const MusaSidebar({
    super.key,
    required this.selectedSection,
    required this.onSectionSelected,
    this.profilePictureUrl,
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
            style: AppTextStyles.logo.copyWith(color: AppColors.ink),
          ),

          const SizedBox(height: 38),

          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _SidebarItem(
                  icon: Icons.home_outlined,
                  title: 'Inicio',
                  selected: selectedSection == AppSection.home,
                  onTap: () => onSectionSelected(AppSection.home),
                ),
                _SidebarItem(
                  icon: Icons.explore_outlined,
                  title: 'Explorar',
                  selected: selectedSection == AppSection.explore,
                  onTap: () => onSectionSelected(AppSection.explore),
                ),
                _SidebarItem(
                  icon: Icons.music_note_outlined,
                  title: 'Música',
                  selected: selectedSection == AppSection.music,
                  onTap: () => onSectionSelected(AppSection.music),
                ),
                _SidebarItem(
                  icon: Icons.menu_book_outlined,
                  title: 'Libros',
                  selected: selectedSection == AppSection.books,
                  onTap: () => onSectionSelected(AppSection.books),
                ),
                _SidebarItem(
                  icon: Icons.people_outline,
                  title: 'Social',
                  selected: selectedSection == AppSection.social,
                  onTap: () => onSectionSelected(AppSection.social),
                ),
                _SidebarItem(
                  icon: Icons.forum_outlined,
                  title: 'Comunidades',
                  selected: selectedSection == AppSection.communities,
                  onTap: () => onSectionSelected(AppSection.communities),
                ),
                _SidebarItem(
                  icon: Icons.bar_chart_outlined,
                  title: 'Estadísticas',
                  selected: selectedSection == AppSection.statistics,
                  onTap: () => onSectionSelected(AppSection.statistics),
                ),
              ],
            ),
          ),

          const Divider(color: AppColors.border),

          const SizedBox(height: 12),

          Center(
            child: Tooltip(
              message: 'Mi perfil',
              child: Semantics(
                label: 'Mi perfil',
                button: true,
                selected: selectedSection == AppSection.profile,
                child: InkResponse(
                  radius: 28,
                  onTap: () => onSectionSelected(AppSection.profile),
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: selectedSection == AppSection.profile
                            ? AppColors.lavender
                            : Colors.transparent,
                        width: 2,
                      ),
                    ),
                    child: MusaProfileAvatar(
                      imageUrl: profilePictureUrl,
                      radius: 20,
                    ),
                  ),
                ),
              ),
            ),
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
  final VoidCallback onTap;

  const _SidebarItem({
    required this.icon,
    required this.title,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 7),
      child: Material(
        color: selected ? AppColors.lavender : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        child: ListTile(
          dense: true,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          onTap: onTap,
          leading: Icon(icon, color: AppColors.ink),
          title: Text(
            title,
            style: AppTextStyles.navigation.copyWith(
              color: AppColors.ink,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
