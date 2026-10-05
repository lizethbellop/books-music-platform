class UserBook {
  final String externalId;
  final String title;
  final String? author;
  final String? coverUrl;
  final String readingStatus;

  UserBook({
    required this.externalId,
    required this.title,
    this.author,
    this.coverUrl,
    required this.readingStatus,
  });

  factory UserBook.fromJson(Map<String, dynamic> json) {
    return UserBook(
      externalId: json['externalId'],
      title: json['title'],
      author: json['author'],
      coverUrl: json['coverUrl'],
      readingStatus: json['readingStatus'],
    );
  }
}