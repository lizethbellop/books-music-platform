import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/core/network/authenticated_api_client.dart';
import 'package:frontend/features/books/data/services/books_api_service.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';

void main() {
  const userId = '13ab67be-9a43-4abd-a9ad-b197265d2401';

  test(
    'Books library and favorites send the current bearer and UUID',
    () async {
      final paths = <String>[];
      final api = BooksApiService(
        getAccessToken: () async => 'current-token',
        client: MockClient((request) async {
          expect(request.headers['Authorization'], 'Bearer current-token');
          expect(request.url.queryParameters['userId'], userId);
          paths.add(request.url.path);
          return http.Response('[]', 200);
        }),
      );
      await api.getLibrary(userId);
      await api.getFavorites(userId);
      expect(paths, ['/books/library', '/books/favorites']);
    },
  );

  test('Books mutations preserve JSON and send authentication', () async {
    final api = BooksApiService(
      getAccessToken: () async => 'current-token',
      client: MockClient((request) async {
        expect(request.headers['Authorization'], 'Bearer current-token');
        expect(request.url.queryParameters['userId'], userId);
        expect(jsonDecode(request.body), {'status': 'LEYENDO'});
        return http.Response('{}', 200);
      }),
    );
    await api.updateReadingStatus(
      externalId: 'OL1W',
      userId: userId,
      status: 'LEYENDO',
    );
  });

  test('Music own reviews include bearer and the real UUID', () async {
    final api = MusicApiService(
      getAccessToken: () async => 'current-token',
      client: MockClient((request) async {
        expect(request.headers['Authorization'], 'Bearer current-token');
        expect(request.url.path, '/api/music/reviews/user/$userId/reviewed');
        return http.Response('[]', 200);
      }),
    );
    expect(await api.getReviewedMusic(userId: userId), isEmpty);
  });

  test('Client gets the latest token for each request', () async {
    var calls = 0;
    final received = <String?>[];
    final client = AuthenticatedApiClient(
      getAccessToken: () async => 'token-${++calls}',
      client: MockClient((request) async {
        received.add(request.headers['Authorization']);
        return http.Response('{}', 200);
      }),
    );
    await client.get(Uri.parse('http://localhost/books/library'));
    await client.get(Uri.parse('http://localhost/books/favorites'));
    expect(received, ['Bearer token-1', 'Bearer token-2']);
    client.close();
  });

  test('An unavailable session prevents the network request', () async {
    var networkCalls = 0;
    final client = AuthenticatedApiClient(
      getAccessToken: () async => throw StateError('No session'),
      client: MockClient((request) async {
        networkCalls++;
        return http.Response('{}', 200);
      }),
    );
    await expectLater(
      client.get(Uri.parse('http://localhost/books/library')),
      throwsStateError,
    );
    expect(networkCalls, 0);
    client.close();
  });
}
