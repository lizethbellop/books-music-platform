import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/music_detail.dart';
import '../models/music_search_item.dart';

class MusicApiService {
  static const String baseUrl = 'http://localhost:8080/api/music';

  Future<List<MusicSearchItem>> searchMusic(String query) async {
    final uri = Uri.parse(
      '$baseUrl/search?q=${Uri.encodeComponent(query)}',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo buscar música');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map(
          (item) => MusicSearchItem.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList();
  }

  Future<MusicDetail> getMusicDetail({
    required String contentType,
    required String spotifyId,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/$contentType/$spotifyId',
    );

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar el contenido musical');
    }

    return MusicDetail.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<void> rateMusic({
    required int userId,
    required MusicDetail detail,
    required double rating,
  }) async {
    final uri = Uri.parse('$baseUrl/ratings');

    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'userId': userId,
        'spotifyId': detail.spotifyId,
        'contentType': detail.contentType,
        'name': detail.name,
        'artistName': detail.artistName,
        'imageUrl': detail.imageUrl,
        'rating': rating,
      }),
    );

    if (response.statusCode != 200 &&
        response.statusCode != 201) {
      throw Exception('No se pudo guardar la calificación');
    }
  }

  Future<void> addFavorite({
    required int userId,
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse('$baseUrl/favorites');

    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'userId': userId,
        'spotifyId': detail.spotifyId,
        'contentType': detail.contentType,
        'name': detail.name,
        'artistName': detail.artistName,
        'imageUrl': detail.imageUrl,
      }),
    );

    if (response.statusCode != 200 &&
        response.statusCode != 201) {
      throw Exception('No se pudo agregar a favoritos');
    }
  }

  Future<void> createReview({
    required int userId,
    required MusicDetail detail,
    required String reviewText,
  }) async {
    final uri = Uri.parse('$baseUrl/reviews');

    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'userId': userId,
        'spotifyId': detail.spotifyId,
        'contentType': detail.contentType,
        'name': detail.name,
        'artistName': detail.artistName,
        'imageUrl': detail.imageUrl,
        'reviewText': reviewText,
      }),
    );

    if (response.statusCode != 200 &&
        response.statusCode != 201) {
      throw Exception('No se pudo crear la reseña');
    }
  }

  Future<Map<String, dynamic>?> getUserReview({
    required int userId,
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/reviews/user/$userId'
      '?spotifyId=${Uri.encodeQueryComponent(detail.spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(detail.contentType)}',
    );

    final response = await http.get(uri);

    if (response.statusCode == 200) {
      if (response.body.isEmpty || response.body == 'null') {
        return null;
      }

      return jsonDecode(response.body) as Map<String, dynamic>;
    }

    throw Exception('No se pudo consultar la reseña');
  }

  Future<void> updateReview({
    required int reviewId,
    required String reviewText,
  }) async {
    final uri = Uri.parse('$baseUrl/reviews/$reviewId');

    final response = await http.put(
      uri,
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'reviewText': reviewText,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('No se pudo editar la reseña');
    }
  }

  Future<void> deleteReview({
    required int reviewId,
  }) async {
    final uri = Uri.parse('$baseUrl/reviews/$reviewId');

    final response = await http.delete(uri);

    if (response.statusCode != 200 &&
        response.statusCode != 204) {
      throw Exception('No se pudo eliminar la reseña');
    }
  }

  Future<List<Map<String, dynamic>>> getReviews({
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/reviews/content'
      '?spotifyId=${Uri.encodeQueryComponent(detail.spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(detail.contentType)}',
    );

    final response = await http.get(uri);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);

      return data
          .map(
            (item) => item as Map<String, dynamic>,
          )
          .toList();
    }

    throw Exception(
      'No se pudieron consultar las reseñas',
    );
  }
}