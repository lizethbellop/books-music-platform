class BookStatistics {
  final int reviews;
  final double? averageRating;
  final int read;
  final int reading;
  final int toRead;

  const BookStatistics({
    required this.reviews,
    required this.averageRating,
    required this.read,
    required this.reading,
    required this.toRead,
  });

  factory BookStatistics.fromJson(Map<String, dynamic> json) {
    return BookStatistics(
      reviews: (json['reviews'] as num?)?.toInt() ?? 0,
      averageRating: (json['averageRating'] as num?)?.toDouble(),
      read: (json['read'] as num?)?.toInt() ?? 0,
      reading: (json['reading'] as num?)?.toInt() ?? 0,
      toRead: (json['toRead'] as num?)?.toInt() ?? 0,
    );
  }
}