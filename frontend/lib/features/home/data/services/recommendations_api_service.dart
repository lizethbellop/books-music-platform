import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/config/api_config.dart';
import '../../../../core/network/authenticated_api_client.dart';
import '../models/home_book_recommendations.dart';
import '../models/home_music_recommendations.dart';

class RecommendationsApiException implements Exception {
  final String message;
  const RecommendationsApiException(this.message);
}

class RecommendationsApiService {
  final http.Client _client;
  final Map<String, Future<String?>> _musicImages = {};
  final Set<String> _missingMusicImages = {};

  RecommendationsApiService({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = AuthenticatedApiClient(
         client: client,
         getAccessToken: getAccessToken,
       );

  Future<HomeBookRecommendations> getHomeBooks({
    String? referenceBookId,
  }) async {
    final response = await _client
        .get(
          Uri.parse(
            referenceBookId == null
                ? '${ApiConfig.recommendationsUrl}/books/knn/me'
                : '${ApiConfig.recommendationsUrl}/books/${Uri.encodeComponent(referenceBookId)}/similar',
          ),
        )
        .timeout(const Duration(seconds: 22));
    if (response.statusCode == 401 || response.statusCode == 403) {
      throw const RecommendationsApiException(
        'Tu sesión no permite consultar las recomendaciones. Inicia sesión nuevamente.',
      );
    }
    if (response.statusCode != 200) {
      throw const RecommendationsApiException(
        'No pudimos cargar tus recomendaciones. Intenta nuevamente.',
      );
    }
    return HomeBookRecommendations.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<HomeMusicRecommendations> getHomeMusic(
    MusicRecommendationKind kind, {
    String? referenceId,
  }) async {
    final response = await _client
        .get(
          Uri.parse(
            referenceId == null
                ? '${ApiConfig.recommendationsUrl}/music/${kind.name}/me'
                : '${ApiConfig.recommendationsUrl}/music/${kind.name}/${Uri.encodeComponent(referenceId)}/similar',
          ),
        )
        .timeout(const Duration(seconds: 22));
    if (response.statusCode == 401 || response.statusCode == 403) {
      throw const RecommendationsApiException(
        'Tu sesión no permite consultar las recomendaciones. Inicia sesión nuevamente.',
      );
    }
    if (response.statusCode != 200) {
      throw const RecommendationsApiException(
        'No pudimos cargar las sugerencias musicales. Intenta nuevamente.',
      );
    }
    final result = HomeMusicRecommendations.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
    if (result.items.any(
      (recommendation) => recommendation.item.contentType != kind.contentType,
    )) {
      throw const RecommendationsApiException(
        'La respuesta musical no corresponde a esta categoría.',
      );
    }
    return result;
  }

  // Images are presentation data, separate from the recommendation vectors.
  Future<String?> getMusicImage(RecommendedMusic recommendation) {
    final item = recommendation.item;
    if (item.imageUrl?.trim().isNotEmpty ?? false) {
      return Future.value(item.imageUrl);
    }
    final key = '${item.contentType}:${item.spotifyId}';
    return _musicImages.putIfAbsent(
      key,
      () => _loadMusicImage(key, item.contentType, item.spotifyId),
    );
  }

  Future<String?> _loadMusicImage(String key, String type, String id) async {
    String? image;
    try {
      final response = await _client
          .get(
            Uri.parse(
              '${ApiConfig.musicUrl}/${Uri.encodeComponent(type)}/${Uri.encodeComponent(id)}',
            ),
          )
          .timeout(const Duration(seconds: 8));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final value = data['imageUrl'];
        final uri = value is String ? Uri.tryParse(value.trim()) : null;
        if (uri != null &&
            uri.host.isNotEmpty &&
            (uri.scheme == 'https' || uri.scheme == 'http')) {
          image = uri.toString();
        }
      }
    } catch (_) {
      // A missing image must not hide an otherwise valid recommendation.
    }
    if (image == null) _missingMusicImages.add(key);
    return image;
  }

  void retryMissingMusicImages() {
    for (final key in _missingMusicImages) {
      _musicImages.remove(key);
    }
    _missingMusicImages.clear();
  }

  void close() => _client.close();
}
