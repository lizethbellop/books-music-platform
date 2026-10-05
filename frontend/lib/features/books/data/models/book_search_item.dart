class BookSearchItem {
  final String externalId;
  final String title;
  final List<String> authors;
  final int? firstPublishYear;
  final String? coverUrl;

  BookSearchItem({
    required this.externalId,
    required this.title,
    required this.authors,
    this.firstPublishYear,
    this.coverUrl,
  });

  factory BookSearchItem.fromJson(Map<String, dynamic> json) {
    return BookSearchItem(
      externalId: json['externalId'],
      title: json['title'],
      authors: List<String>.from(json['authors'] ?? []),
      firstPublishYear: json['firstPublishYear'],
      coverUrl: json['coverUrl'],
    );
  }
}