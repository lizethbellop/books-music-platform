import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/main.dart';
import 'package:frontend/core/routes/app_routes.dart';
import 'package:frontend/features/auth/data/services/auth_api_service.dart';
import 'package:frontend/features/auth/presentation/screens/forgot_password_screen.dart';
import 'package:frontend/features/auth/presentation/screens/verify_token_screen.dart';
import 'package:frontend/features/auth/presentation/screens/reset_password_screen.dart';

void main() {
  void size(WidgetTester tester) {
    tester.view.physicalSize = const Size(1000, 1000);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
  }

  testWidgets('Login opens Derly recovery screen', (tester) async {
    size(tester);
    await tester.pumpWidget(const MusaApp());
    await tester.tap(find.text('¿Olvidaste tu contraseña?'));
    await tester.pumpAndSettle();
    expect(find.byType(ForgotPasswordScreen), findsOneWidget);
  });

  testWidgets('Email, code and new password flow returns to login', (
    tester,
  ) async {
    size(tester);
    final calls = <String>[];
    final api = AuthApiService(
      client: MockClient((request) async {
        calls.add(request.url.path);
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        if (request.url.path.endsWith('forgot-password')) {
          expect(body['email'], 'ana@example.com');
        } else {
          expect(body['token'], 'recovery-code');
        }
        if (request.url.path.endsWith('reset-password')) {
          expect(body['newPassword'], 'ChangedPassword123!');
          expect(body['confirmNewPassword'], body['newPassword']);
        }
        return http.Response(
          '{"message":"Operación correcta"}',
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
    );
    await tester.pumpWidget(
      MaterialApp(
        initialRoute: AppRoutes.forgotPassword,
        routes: {
          AppRoutes.login: (_) => const Scaffold(body: Text('Login final')),
          AppRoutes.forgotPassword: (_) =>
              ForgotPasswordScreen(authApiService: api),
          AppRoutes.verifyToken: (_) => VerifyTokenScreen(authApiService: api),
          AppRoutes.resetPassword: (_) =>
              ResetPasswordScreen(authApiService: api),
        },
      ),
    );
    await tester.enterText(find.byType(TextFormField), 'ana@example.com');
    await tester.tap(find.text('Enviar token'));
    await tester.pumpAndSettle();
    expect(find.byType(VerifyTokenScreen), findsOneWidget);
    await tester.enterText(find.byType(TextFormField), 'recovery-code');
    await tester.tap(find.widgetWithText(ElevatedButton, 'Verificar token'));
    await tester.pumpAndSettle();
    expect(find.byType(ResetPasswordScreen), findsOneWidget);
    await tester.enterText(
      find.byType(TextFormField).at(0),
      'ChangedPassword123!',
    );
    await tester.enterText(
      find.byType(TextFormField).at(1),
      'ChangedPassword123!',
    );
    await tester.tap(find.text('Cambiar contraseña'));
    await tester.pumpAndSettle();
    expect(find.text('Login final'), findsOneWidget);
    expect(calls, [
      '/api/v1/auth/forgot-password',
      '/api/v1/auth/verify-token',
      '/api/v1/auth/reset-password',
    ]);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Rejected recovery code stays on verification screen', (
    tester,
  ) async {
    size(tester);
    final api = AuthApiService(
      client: MockClient(
        (_) async => http.Response(
          '{"message":"Código inválido o expirado"}',
          401,
          headers: {'content-type': 'application/json; charset=utf-8'},
        ),
      ),
    );
    await tester.pumpWidget(
      MaterialApp(home: VerifyTokenScreen(authApiService: api)),
    );
    await tester.enterText(find.byType(TextFormField), 'expired-code');
    await tester.tap(find.widgetWithText(ElevatedButton, 'Verificar token'));
    await tester.pumpAndSettle();
    expect(find.byType(VerifyTokenScreen), findsOneWidget);
    expect(find.text('Código inválido o expirado'), findsOneWidget);
  });
}
