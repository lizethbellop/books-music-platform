class MusicStatistics {
  final int reviews;
  final int ratings;
  final int favorites;
  final double? averageRating;

  const MusicStatistics({
    required this.reviews,
    required this.ratings,
    required this.favorites,
    required this.averageRating,
  });

  factory MusicStatistics.fromJson(Map<String, dynamic> json) {
    return MusicStatistics(
      reviews: (json['reviews'] as num?)?.toInt() ?? 0,
      ratings: (json['ratings'] as num?)?.toInt() ?? 0,
      favorites: (json['favorites'] as num?)?.toInt() ?? 0,
      averageRating: (json['averageRating'] as num?)?.toDouble(),
    );
  }
}