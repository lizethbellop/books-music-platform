import '../../../music/data/models/music_search_item.dart';

enum MusicRecommendationKind {
  artists('ARTIST'),
  albums('ALBUM'),
  songs('SONG');

  final String contentType;
  const MusicRecommendationKind(this.contentType);
}

class RecommendedMusic {
  final MusicSearchItem item;
  final String reason;
  final String? sourceUrl;

  const RecommendedMusic({
    required this.item,
    required this.reason,
    this.sourceUrl,
  });

  factory RecommendedMusic.fromJson(Map<String, dynamic> json) {
    final names = (json['artistNames'] as List<dynamic>? ?? [])
        .whereType<String>()
        .join(', ');
    String? imageUrl = json['imageUrl'] as String?;
    return RecommendedMusic(
      item: MusicSearchItem(
        spotifyId: json['spotifyId'] as String,
        contentType: json['contentType'] as String,
        name: json['name'] as String,
        artistName: names.isEmpty ? null : names,
        imageUrl: imageUrl,
      ),
      reason: json['reason'] as String? ?? 'Una opción para descubrir',
      sourceUrl: json['sourceUrl'] as String?,
    );
  }
}

class HomeMusicRecommendations {
  final List<RecommendedMusic> items;
  final int seedCount;
  final bool partial;
  final String mode;
  final String preparationStatus;
  bool get isPreparing => preparationStatus == 'pending';

  const HomeMusicRecommendations({
    required this.items,
    required this.seedCount,
    this.partial = false,
    this.mode = 'explore',
    this.preparationStatus = 'ready',
  });

  factory HomeMusicRecommendations.fromJson(Map<String, dynamic> json) =>
      HomeMusicRecommendations(
        items: (json['items'] as List<dynamic>)
            .map(
              (value) =>
                  RecommendedMusic.fromJson(value as Map<String, dynamic>),
            )
            .toList(),
        seedCount: json['seed_count'] as int,
        partial: json['partial'] as bool? ?? false,
        mode: json['mode'] as String? ?? 'explore',
        preparationStatus:
            (json['preparation'] as Map<String, dynamic>?)?['status']
                as String? ??
            'ready',
      );
}
