import 'dart:convert';

import '../../../../core/network/review_usernames.dart';

import 'package:http/http.dart' as http;

import '../../../../core/network/authenticated_api_client.dart';

import '../models/music_detail.dart';
import '../models/music_search_item.dart';
import '../music_item.dart';

class MusicApiService {
  final http.Client _client;

  MusicApiService({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = AuthenticatedApiClient(
         client: client,
         getAccessToken: getAccessToken,
       );

  static const String baseUrl = String.fromEnvironment(
    'MUSIC_API_URL',
    defaultValue: 'http://localhost:8080/api/music',
  );

  Future<List<MusicSearchItem>> searchMusic(String query) async {
    final uri = Uri.parse('$baseUrl/search?q=${Uri.encodeComponent(query)}');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo buscar música');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map((item) => MusicSearchItem.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<MusicItem>> getReviewedMusic({required String userId}) async {
    final uri = Uri.parse('$baseUrl/reviews/user/$userId/reviewed');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudieron consultar las reseñas musicales');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data.map((item) {
      final json = item as Map<String, dynamic>;

      final contentType = json['contentType'] as String? ?? 'SONG';

      final type = switch (contentType) {
        'ALBUM' => MusicType.album,
        'ARTIST' => MusicType.artist,
        _ => MusicType.song,
      };

      final rawReviewDate = json['reviewDate'] as String?;

      return MusicItem(
        spotifyId: json['spotifyId'] as String,
        title: json['name'] as String,
        artist: json['artistName'] as String? ?? '',
        imageUrl: json['imageUrl'] as String? ?? '',
        type: type,
        rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
        reviewDate: _formatReviewDate(rawReviewDate),
      );
    }).toList();
  }

  String? _formatReviewDate(String? value) {
    if (value == null || value.isEmpty) {
      return null;
    }

    final date = DateTime.tryParse(value);

    if (date == null) {
      return value;
    }

    final day = date.day.toString().padLeft(2, '0');

    final month = date.month.toString().padLeft(2, '0');

    return '$day/$month/${date.year}';
  }

  Future<MusicDetail> getMusicDetail({
    required String contentType,
    required String spotifyId,
  }) async {
    final uri = Uri.parse('$baseUrl/$contentType/$spotifyId');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar el contenido musical');
    }

    return MusicDetail.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<void> rateMusic({
    required String userId,
    required MusicDetail detail,
    required double rating,
  }) async {
    final uri = Uri.parse('$baseUrl/ratings');

    final response = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
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

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw Exception('No se pudo guardar la calificación');
    }
  }

  Future<void> addFavorite({
    required String userId,
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse('$baseUrl/favorites');

    final response = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'userId': userId,
        'spotifyId': detail.spotifyId,
        'contentType': detail.contentType,
        'name': detail.name,
        'artistName': detail.artistName,
        'imageUrl': detail.imageUrl,
      }),
    );

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw Exception('No se pudo agregar a favoritos');
    }
  }

  Future<void> removeFavorite({
    required String userId,
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/favorites'
      '?userId=$userId'
      '&spotifyId=${Uri.encodeQueryComponent(detail.spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(detail.contentType)}',
    );

    final response = await _client.delete(uri);

    if (response.statusCode != 200 && response.statusCode != 204) {
      throw Exception('No se pudo quitar de favoritos');
    }
  }

  Future<void> createReview({
    required String userId,
    required MusicDetail detail,
    required String reviewText,
  }) async {
    final uri = Uri.parse('$baseUrl/reviews');

    final response = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
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

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw Exception('No se pudo crear la reseña');
    }
  }

  Future<Map<String, dynamic>?> getUserReview({
    required String userId,
    required MusicDetail detail,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/reviews/user/$userId'
      '?spotifyId=${Uri.encodeQueryComponent(detail.spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(detail.contentType)}',
    );

    final response = await _client.get(uri);

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

    final response = await _client.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'reviewText': reviewText}),
    );

    if (response.statusCode != 200) {
      throw Exception('No se pudo editar la reseña');
    }
  }

  Future<void> deleteReview({required int reviewId}) async {
    final uri = Uri.parse('$baseUrl/reviews/$reviewId');

    final response = await _client.delete(uri);

    if (response.statusCode != 200 && response.statusCode != 204) {
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

    final response = await _client.get(uri);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);

      return resolveReviewUsernames(
        data.map((item) => item as Map<String, dynamic>).toList(),
        _client,
      );
    }

    throw Exception('No se pudieron consultar las reseñas');
  }

  Future<double> getUserRating({
    required String userId,
    required String spotifyId,
    required String contentType,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/ratings/user/$userId'
      '?spotifyId=${Uri.encodeQueryComponent(spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(contentType)}',
    );

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar la calificación');
    }

    return double.tryParse(response.body) ?? 0.0;
  }

  Future<bool> getUserFavorite({
    required String userId,
    required String spotifyId,
    required String contentType,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/favorites/user/$userId'
      '?spotifyId=${Uri.encodeQueryComponent(spotifyId)}'
      '&contentType=${Uri.encodeQueryComponent(contentType)}',
    );

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('No se pudo consultar el estado de favoritos');
    }

    return response.body.trim().toLowerCase() == 'true';
  }
}
