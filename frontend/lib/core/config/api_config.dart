/// Configuración central de las URLs del backend.
abstract final class ApiConfig {
  static const String gatewayUrl = String.fromEnvironment(
    'GATEWAY_URL',
    defaultValue: 'http://localhost:8084',
  );

  static const String authUrl = '$gatewayUrl/api/v1/auth';
  static const String booksUrl = '$gatewayUrl/books';
  static const String musicUrl = '$gatewayUrl/api/music';
  static const String recommendationsUrl = '$gatewayUrl/api/recommendations';
  static const String profileUrl = '$gatewayUrl/api/profiles';
  static const String statisticsUrl = '$gatewayUrl/api/statistics';
  static const String notificationsUrl = '$gatewayUrl/api/v1/notifications';
  static const String chatUrl = '$gatewayUrl/api/v1/chat';
  static const String usersUrl = '$gatewayUrl/api/v1/users';
  static const String usernamesUrl = '$gatewayUrl/api/v1/users/usernames';
}
