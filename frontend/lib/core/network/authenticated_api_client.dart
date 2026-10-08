import 'package:http/http.dart' as http;

import '../../features/auth/data/services/auth_session_manager.dart';

/// Adds the current access token, renewing it through the session manager.
class AuthenticatedApiClient extends http.BaseClient {
  final http.Client _client;
  final Future<String> Function() _getAccessToken;

  AuthenticatedApiClient({
    http.Client? client,
    Future<String> Function()? getAccessToken,
  }) : _client = client ?? http.Client(),
       _getAccessToken =
           getAccessToken ?? AuthSessionManager.instance.getAccessToken;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final token = await _getAccessToken();
    request.headers['Authorization'] = 'Bearer $token';
    request.headers['Accept'] = 'application/json';
    return _client.send(request);
  }

  @override
  void close() => _client.close();
}
