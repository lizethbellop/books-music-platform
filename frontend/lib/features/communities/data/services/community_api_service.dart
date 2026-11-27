import 'dart:convert';
import 'package:http/http.dart' as http;

import '../../../../core/config/api_config.dart';
import '../../../auth/data/services/auth_session_manager.dart';
import '../models/community_model.dart';
import '../models/community_member_model.dart';

class CommunityApiException implements Exception {
  final String message;
  final int? statusCode;

  CommunityApiException(this.message, {this.statusCode});

  @override
  String toString() => message;
}

class CommunityApiService {
  final String baseUrl;
  final Future<String?> Function() getAccessToken;
  final http.Client _client;

  CommunityApiService({
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

  /// GET /api/v1/communities?query=...&type=...
  Future<List<CommunityModel>> searchCommunities({
    String? query,
    String? type,
  }) async {
    try {
      final queryParams = <String, String>{};
      if (query != null && query.trim().isNotEmpty) {
        queryParams['query'] = query.trim();
      }
      if (type != null && type != 'TODAS') {
        queryParams['type'] = type;
      }

      final uri = Uri.parse(baseUrl).replace(
        queryParameters: queryParams.isEmpty ? null : queryParams,
      );

      final response = await _client
          .get(uri, headers: await _buildHeaders())
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final decoded = jsonDecode(utf8.decode(response.bodyBytes));
        final data = _extractList(decoded);
        return data
            .map((json) => CommunityModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo conectar con el servidor de comunidades.');
    }
  }

  /// GET /api/v1/communities/{id}
  Future<CommunityModel> getCommunityById(String communityId) async {
    try {
      final response = await _client
          .get(Uri.parse('$baseUrl/$communityId'), headers: await _buildHeaders())
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        return CommunityModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo obtener el detalle de la comunidad.');
    }
  }

  /// POST /api/v1/communities
  Future<CommunityModel> createCommunity(CommunityCreateRequestModel request) async {
    try {
      final response = await _client
          .post(
            Uri.parse(baseUrl),
            headers: await _buildHeaders(),
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 201 || response.statusCode == 200) {
        return CommunityModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo crear la comunidad.');
    }
  }

  /// PUT /api/v1/communities/{id}
  Future<CommunityModel> updateCommunity(
    String communityId,
    CommunityUpdateRequestModel request,
  ) async {
    try {
      final response = await _client
          .put(
            Uri.parse('$baseUrl/$communityId'),
            headers: await _buildHeaders(),
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        return CommunityModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo actualizar la comunidad.');
    }
  }

  /// DELETE /api/v1/communities/{id}
  Future<void> deleteCommunity(String communityId) async {
    try {
      final response = await _client
          .delete(Uri.parse('$baseUrl/$communityId'), headers: await _buildHeaders())
          .timeout(const Duration(seconds: 15));

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw _handleError(response);
      }
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo eliminar la comunidad.');
    }
  }

  /// POST /api/v1/communities/{communityId}/members
  Future<CommunityMemberModel> joinCommunity(String communityId) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/$communityId/members'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200 || response.statusCode == 201) {
        return CommunityMemberModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo unir a la comunidad.');
    }
  }

  /// DELETE /api/v1/communities/{communityId}/members/me"
  Future<void> leaveCommunity(String communityId) async {
    try {
      final response = await _client
          .delete(
            Uri.parse('$baseUrl/$communityId/members/me'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw _handleError(response);
      }
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo abandonar la comunidad.');
    }
  }

  /// GET /api/v1/communities/{id}/members
  Future<List<CommunityMemberModel>> getCommunityMembers(String communityId) async {
    try {
      final response = await _client
          .get(
            Uri.parse('$baseUrl/$communityId/members'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final decoded = jsonDecode(utf8.decode(response.bodyBytes));
        final data = _extractList(decoded);
        return data
            .map((json) => CommunityMemberModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudieron cargar los miembros.');
    }
  }

  /// PATCH /api/v1/communities/{communityId}/members/{userId}/approve
  Future<CommunityMemberModel> approveMember(
    String communityId,
    String userId,
  ) async {
    try {
      final response = await _client
          .patch(
            Uri.parse('$baseUrl/$communityId/members/$userId/approve'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        return CommunityMemberModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      }
      throw _handleError(response);
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo aprobar la solicitud.');
    }
  }

  /// DELETE /api/v1/communities/{communityId}/members/{userId}
  Future<void> rejectOrRemoveMember(
    String communityId,
    String userId,
  ) async {
    try {
      final response = await _client
          .delete(
            Uri.parse('$baseUrl/$communityId/members/$userId'),
            headers: await _buildHeaders(),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw _handleError(response);
      }
    } catch (e) {
      if (e is CommunityApiException) rethrow;
      throw CommunityApiException('No se pudo rechazar o eliminar al miembro.');
    }
  }
}