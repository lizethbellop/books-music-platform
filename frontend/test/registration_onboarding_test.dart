import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/core/routes/app_routes.dart';
import 'package:frontend/features/auth/data/services/auth_api_service.dart';
import 'package:frontend/features/auth/data/services/auth_session_manager.dart';
import 'package:frontend/features/auth/presentation/screens/register_screen.dart';

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthSessionManager.instance.clearLocalSession();
  });
  for (final loginSucceeds in [true, false]) {
    testWidgets(
      'Registration creates account once and handles auto-login: $loginSucceeds',
      (tester) async {
        tester.view.physicalSize = const Size(900, 1400);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final calls = <String>[];
        final client = MockClient((r) async {
          calls.add(r.url.path);
          if (r.url.path.endsWith('/register')) {
            return http.Response('{"message":"Cuenta creada"}', 201);
          }
          expect(r.url.path.endsWith('/login'), isTrue);
          expect(jsonDecode(r.body)['rememberMe'], false);
          if (!loginSucceeds) {
            return http.Response('{"message":"No disponible"}', 503);
          }
          return http.Response(
            jsonEncode({
              'accessToken': 'test-only',
              'refreshToken': 'refresh-only',
              'userId': '438b4a66-07c2-42ad-863c-853e0f151901',
              'username': 'nuevo',
              'fullName': 'Nuevo Usuario',
              'roleName': 'USUARIO',
              'accessTokenExpiresAt': DateTime.now()
                  .toUtc()
                  .add(const Duration(hours: 1))
                  .toIso8601String(),
              'sessionExpiresAt': DateTime.now()
                  .toUtc()
                  .add(const Duration(days: 1))
                  .toIso8601String(),
            }),
            200,
          );
        });
        addTearDown(client.close);
        await tester.pumpWidget(
          MaterialApp(
            home: RegisterScreen(authApi: AuthApiService(client: client)),
            routes: {
              AppRoutes.preferencesOnboarding: (_) =>
                  const Scaffold(body: Text('Elegir preferencias')),
              AppRoutes.login: (context) => Scaffold(
                body: Text(
                  'Login:${ModalRoute.of(context)?.settings.arguments}',
                ),
              ),
            },
          ),
        );
        final values = [
          'Nuevo Usuario',
          'nuevo',
          'nuevo@example.com',
          'Secreto123',
          'Secreto123',
        ];
        for (var i = 0; i < values.length; i++) {
          await tester.enterText(find.byType(TextFormField).at(i), values[i]);
        }
        await tester.ensureVisible(find.text('Crear cuenta'));
        await tester.runAsync(() async {
          await tester.tap(find.text('Crear cuenta'));
          await Future<void>.delayed(const Duration(milliseconds: 80));
        });
        await tester.pumpAndSettle();
        expect(calls.where((s) => s.endsWith('/register')).length, 1);
        expect(calls.where((s) => s.endsWith('/login')).length, 1);
        if (loginSucceeds) {
          expect(find.text('Elegir preferencias'), findsOneWidget);
          expect(AuthSessionManager.instance.session?.username, 'nuevo');
        } else {
          expect(find.text('Login:true'), findsOneWidget);
          expect(AuthSessionManager.instance.session, isNull);
        }
        expect(tester.takeException(), isNull);
      },
    );
  }
}
