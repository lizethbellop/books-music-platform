import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/notifications/data/notification_api.dart';
import 'package:frontend/features/notifications/presentation/notifications_page.dart';
import 'package:frontend/features/auth/data/services/auth_session_manager.dart';
import 'package:frontend/features/auth/data/models/auth_response_model.dart';

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    final now = DateTime.now().toUtc();
    await AuthSessionManager.instance.setSession(
      AuthResponseModel(
        accessToken: 'test',
        refreshToken: 'test',
        userId: 'user',
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
  Map<String, dynamic> item(bool read) => {
    'id': 'notification',
    'actorId': 'actor',
    'type': 'POST_LIKED',
    'title': 'Nuevo me gusta',
    'body': 'A una persona le gustó tu publicación.',
    'targetType': 'POST',
    'targetId': 'post',
    'createdAt': '2026-11-21T20:00:00Z',
    'readAt': read ? '2026-11-21T20:01:00Z' : null,
  };
  testWidgets(
    'lists notifications and marks them read without removing the history',
    (tester) async {
      bool read = false;
      final api = NotificationApi(
        client: MockClient((request) async {
          if (request.method == 'POST') {
            read = true;
            return http.Response('', 204);
          }
          if (request.url.path.endsWith('/unread-count')) {
            return http.Response(jsonEncode({'count': read ? 0 : 1}), 200);
          }
          return http.Response(
            jsonEncode({
              'items': [item(read)],
              'hasMore': false,
              'nextPage': null,
            }),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        }),
      );
      await tester.pumpWidget(MaterialApp(home: NotificationsPage(api: api)));
      await tester.pumpAndSettle();
      expect(find.text('1 sin leer'), findsOneWidget);
      expect(find.text('Nuevo me gusta'), findsOneWidget);
      await tester.tap(find.byTooltip('Marcar como leída'));
      await tester.pumpAndSettle();
      expect(find.text('0 sin leer'), findsOneWidget);
      expect(find.text('Nuevo me gusta'), findsOneWidget);
      expect(find.byTooltip('Marcar como leída'), findsNothing);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
      api.close();
    },
  );
  testWidgets('clears private notification contents on logout', (tester) async {
    final api = NotificationApi(
      client: MockClient(
        (request) async => http.Response(
          request.url.path.endsWith('/unread-count')
              ? '{"count":1}'
              : jsonEncode({
                  'items': [item(false)],
                  'hasMore': false,
                  'nextPage': null,
                }),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        ),
      ),
    );
    await tester.pumpWidget(MaterialApp(home: NotificationsPage(api: api)));
    await tester.pumpAndSettle();
    expect(find.text('Nuevo me gusta'), findsOneWidget);
    await tester.runAsync(AuthSessionManager.instance.clearLocalSession);
    await tester.pumpAndSettle();
    expect(find.text('Nuevo me gusta'), findsNothing);
    expect(
      find.text('Inicia sesión para ver tus notificaciones.'),
      findsOneWidget,
    );
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpAndSettle();
    api.close();
  });
}
