
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/notifications/data/notification_api.dart';

void main() {
  test(
    'notification list never sends a recipient identity from the client',
    () async {
      final api = NotificationApi(
        client: MockClient((request) async {
          expect(request.url.queryParameters, {
            'page': '2',
            'size': '20',
            'unreadOnly': 'true',
          });
          return http.Response(
            '{"items":[],"hasMore":false,"nextPage":null}',
            200,
          );
        }),
      );
      expect((await api.list(page: 2, unreadOnly: true)).items, isEmpty);
      api.close();
    },
  );
  test('mark read accepts a 204 with no body', () async {
    final api = NotificationApi(
      client: MockClient((request) async {
        expect(request.method, 'POST');
        expect(request.url.path.endsWith('/n/read'), isTrue);
        return http.Response('', 204);
      }),
    );
    await api.markRead('n');
    api.close();
  });
  test('does not expose internal error bodies', () async {
    final api = NotificationApi(
      client: MockClient((_) async => http.Response('database secret', 503)),
    );
    await expectLater(
      api.unreadCount(),
      throwsA(
        isA<NotificationApiException>().having(
          (e) => e.message,
          'message',
          isNot(contains('secret')),
        ),
      ),
    );
    api.close();
  });
}
