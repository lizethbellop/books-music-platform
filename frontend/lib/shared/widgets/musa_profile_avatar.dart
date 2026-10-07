import 'package:cached_network_image/cached_network_image.dart';
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

    if (url == null || url.isEmpty) {
      return _placeholder();
    }

    return ClipOval(
      child: CachedNetworkImage(
        imageUrl: url,
        width: radius * 2,
        height: radius * 2,
        fit: BoxFit.cover,
        placeholder: (context, url) => _placeholder(),
        errorWidget: (context, url, error) => _placeholder(),
      ),
    );
  }

  Widget _placeholder() {
    return CircleAvatar(
      radius: radius,
      backgroundColor: AppColors.mint,
      child: Icon(
        Icons.person_outline,
        size: radius * 1.25,
        color: AppColors.ink,
      ),
    );
  }
}