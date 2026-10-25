class ApiConfig {
  static const String gatewayUrl = String.fromEnvironment(
    'GATEWAY_URL',
    defaultValue: 'http://localhost:8084',
  );

  static const String authUrl = '$gatewayUrl/api/v1/auth';

  static const String booksUrl = '$gatewayUrl/books';

  static const String musicUrl = '$gatewayUrl/api/music';

  static const String profileUrl = '$gatewayUrl/api/profiles';
}
