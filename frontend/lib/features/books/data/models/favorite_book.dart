class FavoriteBook {
  final String externalId;
  final String title;
  final String? author;
  final String? coverUrl;

  FavoriteBook({
    required this.externalId,
    required this.title,
    this.author,
    this.coverUrl,
  });

  factory FavoriteBook.fromJson(Map<String, dynamic> json) {
    return FavoriteBook(
      externalId: json['externalId'],
      title: json['title'],
      author: json['author'],
      coverUrl: json['coverUrl'],
    );
  }
}