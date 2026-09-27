import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

class MusaProfileAvatar extends StatelessWidget {
  final String? imageUrl;
  final double radius;

  const MusaProfileAvatar({
    super.key,
    required this.imageUrl,
    this.radius = 18,
  });

  @override
  Widget build(BuildContext context) {
    final url = imageUrl?.trim();

    return CircleAvatar(
      radius: radius,
      backgroundColor: AppColors.mint,
      child: url == null || url.isEmpty
          ? Icon(
              Icons.person_outline,
              size: radius * 1.25,
              color: AppColors.ink,
            )
          : ClipOval(
              child: Image.network(
                url,
                width: radius * 2,
                height: radius * 2,
                fit: BoxFit.cover,
                loadingBuilder: (context, child, progress) {
                  if (progress == null) return child;
                  return Icon(
                    Icons.person_outline,
                    size: radius * 1.25,
                    color: AppColors.ink,
                  );
                },
                errorBuilder: (context, error, stackTrace) {
                  return Icon(
                    Icons.person_outline,
                    size: radius * 1.25,
                    color: AppColors.ink,
                  );
                },
              ),
            ),
    );
  }
}
