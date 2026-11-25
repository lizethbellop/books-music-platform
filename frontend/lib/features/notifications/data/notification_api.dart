import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/config/api_config.dart';
import '../../../core/network/authenticated_api_client.dart';

class NotificationApiException implements Exception {
  final String message;
  NotificationApiException(this.message);
  @override
  String toString() => message;
}

class AppNotification {
  final String id, actorId, type, title, body, targetType, targetId;
  final DateTime createdAt;
  final DateTime? readAt;
  bool get isRead => readAt != null;
  AppNotification({
    required this.id,
    required this.actorId,
    required this.type,
    required this.title,
    required this.body,
    required this.targetType,
    required this.targetId,
    required this.createdAt,
    this.readAt,
  });
  factory AppNotification.fromJson(Map<String, dynamic> data) =>
      AppNotification(
        id: data['id'] as String,
        actorId: data['actorId'] as String,
        type: data['type'] as String,
        title: data['title'] as String,
        body: data['body'] as String,
        targetType: data['targetType'] as String,
        targetId: data['targetId'] as String,
        createdAt: DateTime.parse(data['createdAt'] as String),
        readAt: data['readAt'] == null
            ? null
            : DateTime.parse(data['readAt'] as String),
      );
  AppNotification asRead() => AppNotification(
    id: id,
    actorId: actorId,
    type: type,
    title: title,
    body: body,
    targetType: targetType,
    targetId: targetId,
    createdAt: createdAt,
    readAt: readAt ?? DateTime.now().toUtc(),
  );
}

class NotificationPageData {
  final List<AppNotification> items;
  final int? nextPage;
  NotificationPageData(this.items, this.nextPage);
}

class NotificationApi {
  final http.Client _client;
  final String baseUrl;
  NotificationApi({
    http.Client? client,
    this.baseUrl = ApiConfig.notificationsUrl,
  }) : _client = client ?? AuthenticatedApiClient();
  Future<dynamic> _request(String method, Uri uri) async {
    try {
      final response = await (() async {
        final request = http.Request(method, uri)
          ..headers['Accept'] = 'application/json';
        return http.Response.fromStream(await _client.send(request));
      })().timeout(const Duration(seconds: 12));
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw NotificationApiException(switch (response.statusCode) {
          401 => 'Inicia sesión nuevamente para ver tus notificaciones.',
          404 => 'La notificación no está disponible.',
          503 => 'Las notificaciones no están disponibles en este momento.',
          _ => 'No pudimos completar la operación. Inténtalo de nuevo.',
        });
      }
      return response.statusCode == 204
          ? null
          : jsonDecode(utf8.decode(response.bodyBytes));
    } on TimeoutException {
      throw NotificationApiException(
        'La conexión tardó demasiado. Inténtalo de nuevo.',
      );
    } on http.ClientException {
      throw NotificationApiException(
        'No se pudo conectar con el servicio de notificaciones.',
      );
    } on FormatException {
      throw NotificationApiException(
        'No se pudo leer la respuesta de notificaciones.',
      );
    }
  }

  Future<NotificationPageData> list({
    int page = 0,
    bool unreadOnly = false,
  }) async {
    final data = await _request(
      'GET',
      Uri.parse(baseUrl).replace(
        queryParameters: {
          'page': '$page',
          'size': '20',
          'unreadOnly': '$unreadOnly',
        },
      ),
    ) as Map<String, dynamic>;
    return NotificationPageData(
      (data['items'] as List)
          .map((n) => AppNotification.fromJson(n as Map<String, dynamic>))
          .toList(),
      data['nextPage'] as int?,
    );
  }

  Future<int> unreadCount() async =>
      (await _request('GET', Uri.parse('$baseUrl/unread-count'))
              as Map<String, dynamic>)['count']
          as int;
  Future<void> markRead(String id) async {
    await _request('POST', Uri.parse('$baseUrl/$id/read'));
  }

  Future<void> markAllRead() async {
    await _request('POST', Uri.parse('$baseUrl/read-all'));
  }

  void close() => _client.close();
}
