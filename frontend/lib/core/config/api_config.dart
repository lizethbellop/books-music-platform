/// Punto de entrada único al backend. GATEWAY_URL debe ser el origen, sin
/// barra final ni rutas: --dart-define=GATEWAY_URL=http://localhost:8084
abstract final class ApiConfig {
  static const String gatewayUrl = String.fromEnvironment(
    'GATEWAY_URL',
    defaultValue: 'http://localhost:8084',
  );

  static const String authUrl = '$gatewayUrl/api/v1/auth';
  static const String booksUrl = '$gatewayUrl/books';
  static const String musicUrl = '$gatewayUrl/api/music';
  static const String profileUrl = '$gatewayUrl/api/profiles';
  static const String usernamesUrl = '$gatewayUrl/api/v1/users/usernames';
}
