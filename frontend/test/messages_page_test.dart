import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/chat/data/chat_api.dart';
import 'package:frontend/features/chat/presentation/messages_page.dart';
import 'package:frontend/features/auth/data/models/auth_response_model.dart';
import 'package:frontend/features/auth/data/services/auth_session_manager.dart';

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    final now = DateTime.now().toUtc();
    await AuthSessionManager.instance.setSession(
      AuthResponseModel(
        accessToken: 'test',
        refreshToken: 'refresh',
        userId: 'liz',
        username: 'liz',
        fullName: 'Liz',
        roleName: 'USUARIO',
        accessTokenExpiresAt: now.add(const Duration(minutes: 15)),
        sessionExpiresAt: now.add(const Duration(days: 1)),
      ),
      rememberMe: false,
    );
  });
  tearDown(() async => AuthSessionManager.instance.clearLocalSession());
  testWidgets(
    'opens a private conversation, sends and displays the saved message',
    (tester) async {
      final client = MockClient((request) async {
        final path = request.url.path;
        Object body;
        if (path.endsWith('/realtime/tickets')) {
          return http.Response('{"code":"USERS_SERVICE_UNAVAILABLE"}', 503);
        }
        if (path.endsWith('/usernames')) {
          body = {'liz': 'Liz', 'ana': 'Ana'};
        } else if (path.endsWith('/conversations')) {
          body = {
            'items': [
              {
                'id': 'c',
                'participantIds': ['liz', 'ana'],
              },
            ],
            'hasMore': false,
            'nextPage': null,
          };
        } else if (path.endsWith('/conversations/c')) {
          body = {
            'id': 'c',
            'type': 'PRIVATE',
            'participantIds': ['liz', 'ana'],
          };
        } else if (path.endsWith('/messages') && request.method == 'GET') {
          body = {
            'items': [],
            'hasMore': false,
            'nextCreatedAt': null,
            'nextId': null,
          };
        } else if (path.endsWith('/messages') && request.method == 'POST') {
          body = {
            'id': 'm',
            'conversationId': 'c',
            'senderId': 'liz',
            'content': jsonDecode(request.body)['content'],
            'createdAt': '2026-11-21T20:00:00Z',
          };
        } else {
          return http.Response('{}', 404);
        }
        return http.Response(jsonEncode(body), 200);
      });
      final api = ChatApi(client: client);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(body: MessagesPage(api: api)),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Mensajes'), findsOneWidget);
      await tester.tap(find.text('Ana'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'Hola Ana');
      await tester.tap(find.byTooltip('Enviar mensaje'));
      await tester.pumpAndSettle();
      expect(find.text('Hola Ana'), findsOneWidget);
      expect(
        tester.widget<TextField>(find.byType(TextField)).controller!.text,
        isEmpty,
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      api.close();
    },
  );
}
