class Review {
  final String userId;
  final String externalId;
  final String title;
  final double rating;
  final String? reviewText;
  final DateTime createdAt;
  final DateTime updatedAt;

  Review({
    required this.userId,
    required this.externalId,
    required this.title,
    required this.rating,
    this.reviewText,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Review.fromJson(Map<String, dynamic> json) {
    return Review(
      userId: json['userId'],
      externalId: json['externalId'],
      title: json['title'],
      rating: (json['rating'] as num).toDouble(),
      reviewText: json['reviewText'],
      createdAt: DateTime.parse(json['createdAt']),
      updatedAt: DateTime.parse(json['updatedAt']),
    );
  }
}