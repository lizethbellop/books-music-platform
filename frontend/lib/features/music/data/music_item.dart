enum MusicType {
  song,
  album,
  artist,
}

class MusicItem {
  final String spotifyId;
  final String title;
  final String artist;
  final String imageUrl;
  final MusicType type;
  final double rating;
  final String? reviewDate;

  const MusicItem({
    required this.spotifyId,
    required this.title,
    required this.artist,
    required this.imageUrl,
    required this.type,
    required this.rating,
    this.reviewDate,
  });
}