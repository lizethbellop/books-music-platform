import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/book_detail.dart';
import '../models/book_search_item.dart';
import '../models/review.dart';
import '../models/user_book.dart';
import '../models/favorite_book.dart';

class BooksApiService {
  static const String baseUrl = 'http://localhost:8080/books';

  Future<List<BookSearchItem>> searchBooks(String query) async {
    final uri = Uri.parse(
      '$baseUrl/search?q=${Uri.encodeComponent(query)}',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo buscar libros');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map(
          (item) => BookSearchItem.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList();
  }

  Future<void> removeFavorite({
    required String externalId,
    required String userId,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/$externalId/favorites?userId=$userId',
    );

    final response = await http.delete(uri);

    if (response.statusCode != 204) {
      throw Exception(
        'No se pudo quitar el libro de favoritos',
      );
    }
  }

  Future<List<BookSearchItem>> getExploreBooks() async {
    final uri = Uri.parse(
      '$baseUrl/explore',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception(
        'No se pudo cargar el catálogo',
      );
    }

    final List<dynamic> data =
        jsonDecode(response.body);

    return data
        .map(
          (item) => BookSearchItem.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList();
  }

  Future<List<UserBook>> getLibrary(String userId) async {
    final uri = Uri.parse(
      '$baseUrl/library?userId=$userId',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar la biblioteca');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map(
          (item) => UserBook.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList();
  }

  Future<List<FavoriteBook>> getFavorites(String userId) async {
    final uri = Uri.parse(
      '$baseUrl/favorites?userId=$userId',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudieron consultar los favoritos');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map(
          (item) => FavoriteBook.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList();
  }

  Future<List<Review>> getReviews(String externalId) async {
  final uri = Uri.parse(
    '$baseUrl/$externalId/reviews',
  );

  final response = await http.get(uri);

  if (response.statusCode != 200) {
    throw Exception('No se pudieron consultar las reseñas');
  }

  final List<dynamic> data = jsonDecode(response.body);

  return data
      .map(
        (item) => Review.fromJson(
          item as Map<String, dynamic>,
        ),
      )
      .toList();
}

Future<Review> createReview({
  required String externalId,
  required String userId,
  required double rating,
  String? reviewText,
}) async {
  final uri = Uri.parse(
    '$baseUrl/$externalId/reviews?userId=$userId',
  );

  final response = await http.post(
    uri,
    headers: {
      'Content-Type': 'application/json',
    },
    body: jsonEncode({
      'rating': rating,
      'reviewText': reviewText,
    }),
  );

  if (response.statusCode != 200) {
    throw Exception('No se pudo crear la reseña');
  }

  return Review.fromJson(
    jsonDecode(response.body) as Map<String, dynamic>,
  );
}

Future<Review> updateReview({
  required String externalId,
  required String userId,
  required double rating,
  String? reviewText,
}) async {
  final uri = Uri.parse(
    '$baseUrl/$externalId/reviews?userId=$userId',
  );

  final response = await http.put(
    uri,
    headers: {
      'Content-Type': 'application/json',
    },
    body: jsonEncode({
      'rating': rating,
      'reviewText': reviewText,
    }),
  );

  if (response.statusCode != 200) {
    throw Exception('No se pudo actualizar la reseña');
  }

  return Review.fromJson(
    jsonDecode(response.body) as Map<String, dynamic>,
  );
}

Future<void> deleteReview({
  required String externalId,
  required String userId,
}) async {
  final uri = Uri.parse(
    '$baseUrl/$externalId/reviews?userId=$userId',
  );

  final response = await http.delete(uri);

  if (response.statusCode != 204) {
    throw Exception('No se pudo eliminar la reseña');
  }
}

  Future<void> rateBook({
    required String externalId,
    required String userId,
    required double rating,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/$externalId/rating?userId=$userId',
    );

    final response = await http.put(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'rating': rating,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('No se pudo guardar la calificación');
    }
  }

  Future<BookDetail> getBookDetail(String externalId) async {
    final uri = Uri.parse(
      '$baseUrl/$externalId',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar el libro');
    }

    return BookDetail.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<void> updateReadingStatus({
    required String externalId,
    required String userId,
    required String status,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/$externalId/reading-status?userId=$userId',
    );

    final response = await http.put(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'status': status,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('No se pudo actualizar el estado de lectura');
    }
  }

  Future<void> addFavorite({
    required String externalId,
    required String userId,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/$externalId/favorites?userId=$userId',
    );

    final response = await http.post(uri);

    if (response.statusCode == 200) {
      return;
    }

    if (response.statusCode == 409) {
      throw Exception('El libro ya se encuentra en favoritos');
    }

    throw Exception('No se pudo agregar el libro a favoritos');
  }



}

