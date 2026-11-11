import '../../../books/data/models/book_search_item.dart';

class RecommendedBook {
  final BookSearchItem book;
  final String reason;

  const RecommendedBook({required this.book, required this.reason});

  factory RecommendedBook.fromJson(Map<String, dynamic> json) =>
      RecommendedBook(
        book: BookSearchItem.fromJson(json),
        reason: json['reason'] as String? ?? 'Una lectura para descubrir',
      );
}

class HomeBookRecommendations {
  final List<RecommendedBook> items;
  final int seedCount;
  final bool partial;

  const HomeBookRecommendations({
    required this.items,
    required this.seedCount,
    this.partial = false,
  });

  factory HomeBookRecommendations.fromJson(Map<String, dynamic> json) =>
      HomeBookRecommendations(
        items: (json['items'] as List<dynamic>)
            .map(
              (value) =>
                  RecommendedBook.fromJson(value as Map<String, dynamic>),
            )
            .toList(),
        seedCount: json['seed_count'] as int,
        partial: json['partial'] as bool? ?? false,
      );
}
