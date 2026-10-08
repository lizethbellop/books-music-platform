import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/auth/data/models/login_request_model.dart';
import 'package:frontend/features/auth/data/models/register_request_model.dart';
import 'package:frontend/features/auth/data/models/token_request_model.dart';
import 'package:frontend/features/auth/data/services/auth_api_service.dart';

void main() {
  AuthApiService api(String body, int status) => AuthApiService(
    client: MockClient(
      (_) async => http.Response(
        body,
        status,
        headers: {'content-type': 'application/json; charset=utf-8'},
      ),
    ),
  );
  Matcher failure(int status, String message) => isA<AuthApiException>()
      .having((e) => e.statusCode, 'status', status)
      .having((e) => e.message, 'message', contains(message));

  test(
    'Empty refresh rejection keeps 401 so the session can be cleared',
    () async {
      await expectLater(
        api(
          '',
          401,
        ).refreshToken(const TokenRequestModel(refreshToken: 'test')),
        throwsA(failure(401, '401')),
      );
    },
  );

  test('HTML server failure keeps its HTTP status', () async {
    await expectLater(
      api('<html>Bad gateway</html>', 502).login(
        const LoginRequestModel(email: 'ana@example.com', password: 'test'),
      ),
      throwsA(failure(502, '502')),
    );
  });

  test('Registration shows field validation returned by backend', () async {
    await expectLater(
      api('{"username":"Username inválido"}', 400).register(
        RegisterRequestModel(
          username: 'x',
          fullName: 'Ana',
          email: 'ana@example.com',
          password: 'test',
          confirmPassword: 'test',
          roleName: 'USUARIO',
        ),
      ),
      throwsA(failure(400, 'Username inválido')),
    );
  });

  test('Logout preserves the server message', () async {
    await expectLater(
      api(
        '{"message":"Sesión vencida"}',
        401,
      ).logout(const TokenRequestModel(refreshToken: 'test')),
      throwsA(failure(401, 'Sesión vencida')),
    );
  });
}
