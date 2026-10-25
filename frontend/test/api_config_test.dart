import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/core/config/api_config.dart';
import 'package:frontend/features/auth/data/services/auth_api_service.dart';
import 'package:frontend/features/books/data/services/books_api_service.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/profile/data/services/profile_api_service.dart';

void main() {
  test('All API defaults share the Gateway and preserve existing routes', () {
    final endpoints = {
      AuthApiService().baseUrl: '/api/v1/auth',
      BooksApiService.baseUrl: '/books',
      MusicApiService.baseUrl: '/api/music',
      ProfileApiService.baseUrl: '/api/profiles',
      ApiConfig.usernamesUrl: '/api/v1/users/usernames',
    };
    for (final entry in endpoints.entries) {
      expect(entry.key, '${ApiConfig.gatewayUrl}${entry.value}');
    }
  });
}
