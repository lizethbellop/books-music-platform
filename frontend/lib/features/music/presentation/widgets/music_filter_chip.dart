import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';

class MusicFilterChip extends StatelessWidget {
  final String label;
  final bool selected;

  const MusicFilterChip({
    super.key,
    required this.label,
    this.selected = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 18,
        vertical: 10,
      ),
      decoration: BoxDecoration(
        color: selected
            ? AppColors.ink
            : AppColors.lilac.withValues(alpha: 0.45),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: AppTextStyles.button.copyWith(
          color: selected
              ? AppColors.warmWhite
              : AppColors.ink,
        ),
      ),
    );
  }
}