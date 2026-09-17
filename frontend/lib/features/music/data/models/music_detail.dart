class MusicDetail {
  final String spotifyId;
  final String contentType;
  final String name;
  final String? artistName;
  final String? imageUrl;
  final String? spotifyUrl;
  final String? releaseDate;
  final int? totalTracks;

  MusicDetail({
    required this.spotifyId,
    required this.contentType,
    required this.name,
    this.artistName,
    this.imageUrl,
    this.spotifyUrl,
    this.releaseDate,
    this.totalTracks,
  });

  factory MusicDetail.fromJson(Map<String, dynamic> json) {
    return MusicDetail(
      spotifyId: json['spotifyId'],
      contentType: json['contentType'],
      name: json['name'],
      artistName: json['artistName'],
      imageUrl: json['imageUrl'],
      spotifyUrl: json['spotifyUrl'],
      releaseDate: json['releaseDate'],
      totalTracks: json['totalTracks'],
    );
  }
}