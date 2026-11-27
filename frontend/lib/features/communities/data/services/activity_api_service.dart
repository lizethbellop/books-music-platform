import 'dart:convert';
import 'package:http/http.dart' as http;

import '../../../../core/config/api_config.dart';
import '../../../auth/data/services/auth_session_manager.dart';
import '../models/activity_model.dart';
import '../models/participant_model.dart';
import 'community_api_service.dart';

class ActivityApiService {
  final String baseUrl;
  final Future<String?> Function() getAccessToken;
  final http.Client _client;

  ActivityApiService({
    this.baseUrl = ApiConfig.communitiesUrl,
    Future<String?> Function()? getAccessToken,
    http.Client? client,
  })  : getAccessToken = getAccessToken ?? AuthSessionManager.instance.getAccessToken,
        _client = client ?? http.Client();

  Future<Map<String, String>> _buildHeaders() async {
    final token = await getAccessToken();
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };
  }

  CommunityApiException _handleError(http.Response response) {
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map<String, dynamic>) {
        final message = decoded['message'];
        if (message is String && message.isNotEmpty) {
          return CommunityApiException(message, statusCode: response.statusCode);
        }
      }
    } on FormatException {
      // Mantiene el estado HTTP real si el cuerpo no es JSON
    }
    return CommunityApiException(
      'El servidor rechazó la solicitud (${response.statusCode}).',
      statusCode: response.statusCode,
    );
  }

  List<dynamic> _extractList(dynamic decoded) {
    if (decoded is List<dynamic>) {
      return decoded;
    }
    if (decoded is Map<String, dynamic> && decoded['content'] is List<dynamic>) {
      return decoded['content'] as List<dynamic>;
    }
    return const [];
  }
  /// GET /api/v1/communities/{communityId}/activities
  Future<List<ActivityModel>> getCommunityActivities(String communityId) async {
    try {
      final response = await _client
          .get(
            Uri.parse('$baseUrl/$communityId/activities'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final decoded = jsonDecode(utf8.decode(response.bodyBytes));
        final data = _extractList(decoded);
        return data
            .map((json) => ActivityModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudieron cargar las actividades.');
    }
  }

  /// POST /api/v1/communities/{communityId}/activities
  Future<ActivityModel> createActivity(
    String communityId,
    ActivityCreateRequestModel request,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/$communityId/activities'),
            headers: await _buildHeaders(),
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 201 || response.statusCode == 200) {
        return ActivityModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo crear la actividad.');
    }
  }

  /// PUT /api/v1/communities/{communityId}/activities/{activityId}
  Future<ActivityModel> updateActivity(
    String communityId,
    String activityId,
    ActivityUpdateRequestModel request,
  ) async {
    try {
      final response = await _client
          .put(
            Uri.parse('$baseUrl/$communityId/activities/$activityId'),
            headers: await _buildHeaders(),
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        return ActivityModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo actualizar la actividad.');
    }
  }

  /// DELETE /api/v1/communities/{communityId}/activities/{activityId}
  Future<void> deleteActivity(String communityId, String activityId) async {
    try {
      final response = await _client
          .delete(
            Uri.parse('$baseUrl/$communityId/activities/$activityId'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw _handleError(response);
      }
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo eliminar la actividad.');
    }
  }

  /// POST /api/v1/communities/{communityId}/activities/{activityId}/participants
  Future<ParticipantModel> confirmAttendance(
    String communityId,
    String activityId,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/$communityId/activities/$activityId/participants'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ParticipantModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo confirmar tu asistencia.');
    }
  }

  /// DELETE /api/v1/communities/{communityId}/activities/{activityId}/participants
  Future<void> cancelAttendance(String communityId, String activityId) async {
    try {
      final response = await _client
          .delete(
            Uri.parse('$baseUrl/$communityId/activities/$activityId/participants'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw _handleError(response);
      }
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo cancelar tu asistencia.');
    }
  }
}