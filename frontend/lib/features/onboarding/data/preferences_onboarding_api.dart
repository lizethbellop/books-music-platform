import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/config/api_config.dart';
import '../../../core/network/authenticated_api_client.dart';

class PreferencesOnboardingApiException implements Exception {
  final String message;
  const PreferencesOnboardingApiException(this.message);
}

class PreferencesOnboardingApi {
  final http.Client _client;
  PreferencesOnboardingApi({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = AuthenticatedApiClient(
         client: client,
         getAccessToken: getAccessToken,
       );

  Future<void> ensureProfile() async {
    final response = await _client
        .post(Uri.parse('${ApiConfig.profileUrl}/me'))
        .timeout(const Duration(seconds: 15));
    if (response.statusCode != 200) {
      throw const PreferencesOnboardingApiException(
        'No pudimos preparar tu perfil. Puedes reintentar o continuar por ahora.',
      );
    }
  }

  Future<void> save(List<Map<String, String>> selections) async {
    final response = await _client
        .post(
          Uri.parse('${ApiConfig.profileUrl}/me/preferences/batch'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'elements': selections}),
        )
        .timeout(const Duration(seconds: 20));
    if (response.statusCode != 200) {
      throw const PreferencesOnboardingApiException(
        'No pudimos confirmar el guardado. Tu selección sigue aquí; puedes reintentar sin duplicarla.',
      );
    }
  }

  void close() => _client.close();
}
