import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/models/music_search_item.dart';
import '../../data/music_item.dart';
import '../pages/music_detail_page.dart';

class MusicCard extends StatelessWidget {
  final String userId;
  final MusicItem item;
  final Future<void> Function()? onReturn;

  const MusicCard({
    super.key,
    required this.item,
    required this.userId,
    this.onReturn,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 190,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () async {
            final searchItem = MusicSearchItem(
              spotifyId: item.spotifyId,
              contentType: _contentType(item.type),
              name: item.title,
              artistName: item.artist,
              imageUrl: item.imageUrl,
            );

            await Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) {
                  return MusicDetailPage(
                    userId: userId,
                    item: searchItem,
                  );
                },
              ),
            );

            await onReturn?.call();
          },
          child: Container(
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
                              errorBuilder: (_, _, _) {
                                return const Icon(
                                  Icons.album_outlined,
                                  size: 56,
                                  color: AppColors.ink,
                                );
                              },
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
                  children: List.generate(
                    5,
                    (index) {
                      final fullValue =
                          (index + 1).toDouble();

                      return Icon(
                        item.rating >= fullValue
                            ? Icons.star
                            : item.rating >= fullValue - 0.5
                                ? Icons.star_half
                                : Icons.star_border,
                        size: 18,
                        color: AppColors.butter,
                      );
                    },
                  ),
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
          ),
        ),
      ),
    );
  }

  String _contentType(MusicType type) {
    return switch (type) {
      MusicType.song => 'SONG',
      MusicType.album => 'ALBUM',
      MusicType.artist => 'ARTIST',
    };
  }
}