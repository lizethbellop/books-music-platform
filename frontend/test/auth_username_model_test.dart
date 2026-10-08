import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/features/auth/data/models/auth_response_model.dart';
import 'package:frontend/features/auth/data/models/register_request_model.dart';

void main() {
  Map<String, dynamic> response() => {
    'accessToken': 'access',
    'refreshToken': 'refresh',
    'userId': 'uuid',
    'fullName': 'Liz',
    'roleName': 'USUARIO',
    'username': 'liz_bello',
    'accessTokenExpiresAt': '2026-10-08T02:15:00Z',
    'sessionExpiresAt': '2026-10-15T02:00:00Z',
  };
  test('Registration sends username alongside the existing fields', () {
    final request = RegisterRequestModel(
      username: 'liz_bello',
      fullName: 'Liz',
      email: 'liz@example.com',
      password: 'test',
      confirmPassword: 'test',
      roleName: 'USUARIO',
    );
    expect(request.toJson()['username'], 'liz_bello');
    expect(request.toJson()['roleName'], 'USUARIO');
  });
  test('Session serialization preserves username', () {
    final session = AuthResponseModel.fromJson(response());
    final restored = AuthResponseModel.fromJson(session.toJson());
    expect(restored.username, 'liz_bello');
    expect(restored.userId, session.userId);
  });
  test('Legacy stored sessions require renewal to obtain username', () {
    final old = response()..remove('username');
    final session = AuthResponseModel.fromJson(old);
    expect(session.username, isEmpty);
    expect(session.accessTokenExpiresAt.isBefore(DateTime.now()), isTrue);
  });
}
