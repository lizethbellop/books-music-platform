import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/chat/data/chat_api.dart';
import 'package:frontend/features/chat/data/chat_models.dart';

void main() {
  test(
    'history preserves exact cursor and encodes it without losing precision',
    () async {
      const cursor = '2026-11-21T20:00:00.123456Z';
      final api = ChatApi(
        client: MockClient((request) async {
          expect(request.url.queryParameters['beforeCreatedAt'], cursor);
          expect(request.url.queryParameters['beforeId'], 'message-id');
          return http.Response(
            jsonEncode({
              'items': [],
              'hasMore': false,
              'nextCreatedAt': null,
              'nextId': null,
            }),
            200,
          );
        }),
      );
      await api.history(
        'conversation-id',
        beforeCreatedAt: cursor,
        beforeId: 'message-id',
      );
      api.close();
    },
  );
  test('history model keeps cursor string exactly', () {
    const cursor = '2026-11-21T20:00:00.123456Z';
    final page = ChatHistory.fromJson({
      'items': [],
      'hasMore': true,
      'nextCreatedAt': cursor,
      'nextId': 'id',
    });
    expect(page.nextCreatedAt, cursor);
  });
  test('send does not let the app substitute sender identity', () async {
    final api = ChatApi(
      client: MockClient((request) async {
        expect(jsonDecode(request.body), {'content': 'Hola'});
        return http.Response(
          jsonEncode({
            'id': 'm',
            'conversationId': 'c',
            'senderId': 'server-user',
            'content': 'Hola',
            'createdAt': '2026-11-21T20:00:00Z',
          }),
          200,
        );
      }),
    );
    expect((await api.send('c', 'Hola')).senderId, 'server-user');
    api.close();
  });
  test('does not expose backend internals in error responses', () async {
    final api = ChatApi(
      client: MockClient(
        (_) async => http.Response(
          '{"code":"CHAT_STORAGE_UNAVAILABLE","message":"postgres-password-secret"}',
          503,
        ),
      ),
    );
    await expectLater(
      api.conversations(),
      throwsA(
        isA<ChatApiException>().having(
          (e) => e.message,
          'message',
          isNot(contains('postgres-password-secret')),
        ),
      ),
    );
    api.close();
  });
  test('handles a non JSON upstream failure with a safe message', () async {
    final api = ChatApi(
      client: MockClient(
        (_) async => http.Response('<html>proxy internals</html>', 502),
      ),
    );
    await expectLater(
      api.conversations(),
      throwsA(isA<ChatApiException>().having((e) => e.status, 'status', 502)),
    );
    api.close();
  });
}
