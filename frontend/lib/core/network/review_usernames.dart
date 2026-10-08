import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../features/auth/data/services/auth_session_manager.dart';

/// Resolves public author names in batches without exposing email or role.
Future<List<Map<String, dynamic>>> resolveReviewUsernames(
  List<Map<String, dynamic>> reviews,
  http.Client authenticatedClient,
) async {
  final ids = reviews
      .map((review) => review['userId'])
      .whereType<String>()
      .where(
        (id) => RegExp(
          r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
        ).hasMatch(id),
      )
      .toSet()
      .toList();
  final names = <String, String>{};
  final session = AuthSessionManager.instance.session;
  if (session != null && session.username.isNotEmpty) {
    names[session.userId] = session.username;
  }
  const authUrl = String.fromEnvironment(
    'AUTH_API_URL',
    defaultValue: 'http://localhost:8083/api/v1/auth',
  );
  final endpoint = Uri.parse(authUrl).replace(
    path: Uri.parse(authUrl).path
        .replaceFirst(RegExp(r'/auth/?$'), '/users/usernames'),
  );
  for (var start = 0; start < ids.length; start += 100) {
    final batch = ids.skip(start).take(100).join(',');
    try {
      final response = await authenticatedClient
          .get(endpoint.replace(queryParameters: {'ids': batch}))
          .timeout(const Duration(seconds: 3));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        data.forEach((id, name) {
          if (name is String && name.trim().isNotEmpty) names[id] = name;
        });
      }
    } catch (_) {
      // Keep reviews usable if the user directory is temporarily unavailable.
    }
  }
  return reviews
      .map(
        (review) => <String, dynamic>{
          ...review,
          'username': names[review['userId']] ?? 'Usuario',
        },
      )
      .toList();
}
