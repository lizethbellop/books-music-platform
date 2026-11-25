import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/config/api_config.dart';
import '../../../core/network/authenticated_api_client.dart';
import 'chat_models.dart';

class ChatApiException implements Exception {
  final String message;
  final int? status;
  ChatApiException(this.message, [this.status]);
  @override
  String toString() => message;
}

class ChatApi {
  final http.Client _client;
  final String baseUrl, usersUrl;
  ChatApi({
    http.Client? client,
    this.baseUrl = ApiConfig.chatUrl,
    this.usersUrl = ApiConfig.usersUrl,
  }) : _client = client ?? AuthenticatedApiClient();

  Future<dynamic> _request(
    String method,
    Uri uri, [
    Map<String, dynamic>? body,
  ]) async {
    try {
      final request = http.Request(method, uri);
      request.headers['Content-Type'] = 'application/json';
      if (body != null) request.body = jsonEncode(body);
      final response = await (() async {
        final streamed = await _client.send(request);
        return http.Response.fromStream(streamed);
      })().timeout(const Duration(seconds: 15));
      if (response.statusCode == 204) return null;
      dynamic data;
      try {
        data = jsonDecode(utf8.decode(response.bodyBytes));
      } on FormatException {
        data = null;
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        final code = data is Map ? data['code'] : null;
        final messages = <String, String>{
          'GROUP_ADMIN_REQUIRED':
              'Solo la persona administradora puede realizar esta acción.',
          'INVALID_REQUEST':
              'Revisa el nombre, los participantes y los límites del grupo.',
          'SELF_CONVERSATION_NOT_ALLOWED':
              'Elige a otra persona para conversar.',
          'RECIPIENT_UNAVAILABLE': 'Ese usuario no está disponible.',
          'CONVERSATION_UNAVAILABLE': 'No puedes acceder a esta conversación.',
          'CONVERSATION_BUSY':
              'La conversación está ocupada. Inténtalo de nuevo.',
          'USERS_SERVICE_UNAVAILABLE':
              'No podemos verificar los usuarios. Inténtalo más tarde.',
          'CHAT_STORAGE_UNAVAILABLE':
              'El chat no está disponible en este momento.',
          'VALIDATION_ERROR':
              'Revisa el mensaje y los datos de la conversación.',
        };
        throw ChatApiException(
          messages[code] ??
              (response.statusCode == 401
                  ? 'Tu sesión venció. Inicia sesión nuevamente.'
                  : 'No pudimos completar la operación. Inténtalo de nuevo.'),
          response.statusCode,
        );
      }
      if (data == null) {
        throw ChatApiException('El chat devolvió una respuesta no válida.');
      }
      return data;
    } on TimeoutException {
      throw ChatApiException(
        'La conexión tardó demasiado. Revisa el historial antes de reenviar.',
      );
    } on http.ClientException {
      throw ChatApiException('No se pudo conectar con el chat.');
    } on FormatException {
      throw ChatApiException('El chat devolvió una respuesta no válida.');
    }
  }

  Future<ChatConversation> details(String id) async =>
      ChatConversation.fromJson(
        await _request('GET', Uri.parse('$baseUrl/conversations/$id'))
            as Map<String, dynamic>,
      );
  Future<ChatConversation> createGroup(
    String name,
    List<String> participantIds,
  ) async => ChatConversation.fromJson(
    await _request('POST', Uri.parse('$baseUrl/conversations/groups'), {
      'name': name,
      'participantIds': participantIds,
    }) as Map<String, dynamic>,
  );
  Future<ChatConversation> renameGroup(String id, String name) async =>
      ChatConversation.fromJson(
        await _request('PATCH', Uri.parse('$baseUrl/conversations/$id/group'), {
          'name': name,
        }) as Map<String, dynamic>,
      );
  Future<ChatConversation> addParticipant(String id, String userId) async =>
      ChatConversation.fromJson(
        await _request(
          'POST',
          Uri.parse('$baseUrl/conversations/$id/participants'),
          {'userId': userId},
        ) as Map<String, dynamic>,
      );
  Future<ChatConversation> removeParticipant(String id, String userId) async =>
      ChatConversation.fromJson(
        await _request(
          'DELETE',
          Uri.parse('$baseUrl/conversations/$id/participants/$userId'),
        ) as Map<String, dynamic>,
      );
  Future<ChatConversation> transferAdmin(String id, String userId) async =>
      ChatConversation.fromJson(
        await _request('POST', Uri.parse('$baseUrl/conversations/$id/admin'), {
          'userId': userId,
        }) as Map<String, dynamic>,
      );
  Future<void> leaveGroup(String id) async {
    await _request('POST', Uri.parse('$baseUrl/conversations/$id/leave'));
  }

  Future<ChatConversation> create(String recipientId) async =>
      ChatConversation.fromJson(
        await _request('POST', Uri.parse('$baseUrl/conversations/private'), {
          'recipientId': recipientId,
        }) as Map<String, dynamic>,
      );
  Future<ChatConversationPage> conversations({int page = 0}) async =>
      ChatConversationPage.fromJson(
        await _request(
          'GET',
          Uri.parse('$baseUrl/conversations')
              .replace(queryParameters: {'page': '$page', 'size': '20'}),
        ) as Map<String, dynamic>,
      );
  Future<ChatHistory> history(
    String conversationId, {
    String? beforeCreatedAt,
    String? beforeId,
  }) async => ChatHistory.fromJson(
    await _request(
      'GET',
      Uri.parse('$baseUrl/conversations/$conversationId/messages').replace(
        queryParameters: {
          'size': '30',
          'beforeCreatedAt': ?beforeCreatedAt,
          'beforeId': ?beforeId,
        },
      ),
    ) as Map<String, dynamic>,
  );
  Future<ChatMessage> send(String conversationId, String content) async =>
      ChatMessage.fromJson(
        await _request(
          'POST',
          Uri.parse('$baseUrl/conversations/$conversationId/messages'),
          {'content': content},
        ) as Map<String, dynamic>,
      );
  Future<String> ticket() async =>
      (await _request('POST', Uri.parse('$baseUrl/realtime/tickets'))
              as Map<String, dynamic>)['ticket']
          as String;
  Future<List<ChatRecipient>> search(String query) async {
    final data = await _request(
      'GET',
      Uri.parse('$usersUrl/search').replace(queryParameters: {'q': query}),
    ) as List;
    return data
        .map((u) => ChatRecipient(u['id'] as String, u['username'] as String))
        .toList();
  }

  Future<Map<String, String>> usernames(Iterable<String> ids) async {
    if (ids.isEmpty) return {};
    final data = await _request(
      'GET',
      Uri.parse('$usersUrl/usernames')
          .replace(queryParameters: {'ids': ids.toSet().join(',')}),
    );
    return Map<String, String>.from(data as Map);
  }

  void close() => _client.close();
}
