import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/music_item.dart';

class MusicCard extends StatelessWidget {
  final MusicItem item;

  const MusicCard({
    super.key,
    required this.item,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 190,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.warmWhite,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 1,
            child: Container(
              decoration: BoxDecoration(
                color: AppColors.lilac,
                borderRadius: BorderRadius.circular(12),
              ),
              child: item.imageUrl.isEmpty
                  ? const Icon(
                      Icons.album_outlined,
                      size: 56,
                      color: AppColors.ink,
                    )
                  : ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.network(
                        item.imageUrl,
                        fit: BoxFit.cover,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            item.title,
            style: AppTextStyles.cardTitle,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 4),
          Text(
            item.artist,
            style: AppTextStyles.secondary,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
          Row(
            children: List.generate(5, (index) {
              return Icon(
                index < item.rating.round()
                    ? Icons.star
                    : Icons.star_border,
                size: 18,
                color: AppColors.butter,
              );
            }),
          ),
          if (item.reviewDate != null) ...[
            const SizedBox(height: 6),
            Text(
              item.reviewDate!,
              style: AppTextStyles.secondary,
            ),
          ],
        ],
      ),
    );
  }
}