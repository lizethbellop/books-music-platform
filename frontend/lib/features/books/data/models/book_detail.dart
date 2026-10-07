class BookDetail {
  final String externalId;
  final String title;
  final List<String> authors;
  final String? description;
  final String? firstPublishDate;
  final List<String> subjects;
  final String? coverUrl;

  BookDetail({
    required this.externalId,
    required this.title,
    required this.authors,
    this.description,
    this.firstPublishDate,
    required this.subjects,
    this.coverUrl,
  });

  factory BookDetail.fromJson(Map<String, dynamic> json) {
    return BookDetail(
      externalId: json['externalId'],
      title: json['title'],
      authors: List<String>.from(json['authors'] ?? []),
      description: json['description'],
      firstPublishDate: json['firstPublishDate'],
      subjects: List<String>.from(json['subjects'] ?? []),
      coverUrl: json['coverUrl'],
    );
  }
}