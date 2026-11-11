import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../../core/config/api_config.dart';
import '../../../../core/network/authenticated_api_client.dart';
import '../models/global_statistics.dart';
import '../models/personal_statistics.dart';

class StatisticsApiService {
  final http.Client _client;

  StatisticsApiService({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = AuthenticatedApiClient(
         client: client,
         getAccessToken: getAccessToken,
       );

  static const String baseUrl = ApiConfig.statisticsUrl;

  Future<PersonalStatistics> getPersonalStatistics() async {
    final uri = Uri.parse('$baseUrl/personal');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception(
        'No se pudieron consultar las estadísticas personales',
      );
    }

    return PersonalStatistics.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<GlobalStatistics> getGlobalStatistics() async {
    final uri = Uri.parse('$baseUrl/global');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception(
        'No se pudieron consultar las estadísticas globales',
      );
    }

    return GlobalStatistics.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }
}