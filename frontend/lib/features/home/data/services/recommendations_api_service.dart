import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/config/api_config.dart';
import '../../../../core/network/authenticated_api_client.dart';
import '../models/home_book_recommendations.dart';

class RecommendationsApiException implements Exception {
  final String message;
  const RecommendationsApiException(this.message);
}

class RecommendationsApiService {
  final http.Client _client;

  RecommendationsApiService({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = AuthenticatedApiClient(
         client: client,
         getAccessToken: getAccessToken,
       );

  Future<HomeBookRecommendations> getHomeBooks() async {
    final response = await _client
        .get(Uri.parse('${ApiConfig.recommendationsUrl}/books/me'))
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

  void close() => _client.close();
}
