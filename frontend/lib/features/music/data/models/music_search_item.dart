class MusicSearchItem {
  final String spotifyId;
  final String contentType;
  final String name;
  final String? artistName;
  final String? imageUrl;

  MusicSearchItem({
    required this.spotifyId,
    required this.contentType,
    required this.name,
    this.artistName,
    this.imageUrl,
  });

  factory MusicSearchItem.fromJson(Map<String, dynamic> json) {
    return MusicSearchItem(
      spotifyId: json['spotifyId'],
      contentType: json['contentType'],
      name: json['name'],
      artistName: json['artistName'],
      imageUrl: json['imageUrl'],
    );
  }
}