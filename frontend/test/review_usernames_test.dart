import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/core/network/review_usernames.dart';
import 'package:frontend/core/network/authenticated_api_client.dart';
import 'package:frontend/features/books/data/services/books_api_service.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/music/data/models/music_detail.dart';

void main() {
  const id = '438b4a66-07c2-42ad-863c-853e0f151901';
  test(
    'Resolves names once per batch while keeping review UUIDs and bearer',
    () async {
      var calls = 0;
      final client = AuthenticatedApiClient(
        getAccessToken: () async => 'access',
        client: MockClient((request) async {
          calls++;
          expect(request.url.path, '/api/v1/users/usernames');
          expect(request.url.queryParameters['ids'], id);
          expect(request.headers['Authorization'], 'Bearer access');
          return http.Response(jsonEncode({id: 'liz_bello'}), 200);
        }),
      );
      final results = await resolveReviewUsernames([
        {'userId': id},
        {'userId': id},
      ], client);
      expect(calls, 1);
      expect(
        results.every(
          (review) =>
              review['username'] == 'liz_bello' && review['userId'] == id,
        ),
        isTrue,
      );
    },
  );
  test(
    'Unavailable directory preserves reviews without displaying UUIDs',
    () async {
      final result = await resolveReviewUsernames([
        {'userId': id, 'reviewText': 'Texto'},
      ], MockClient((_) async => http.Response('', 503)));
      expect(result.single['username'], 'Usuario');
      expect(result.single['reviewText'], 'Texto');
    },
  );
  test('Book reviews receive the public username', () async {
    final service = BooksApiService(
      getAccessToken: () async => 'access',
      client: MockClient((request) async {
        if (request.url.path.endsWith('/usernames')) {
          return http.Response(jsonEncode({id: 'liz_bello'}), 200);
        }
        return http.Response(
          jsonEncode([
            {
              'userId': id,
              'externalId': 'OL1W',
              'title': 'Libro',
              'rating': 4,
              'reviewText': 'Reseña',
              'createdAt': '2026-10-08T00:00:00',
              'updatedAt': '2026-10-08T00:00:00',
            },
          ]),
          200,
        );
      }),
    );
    expect((await service.getReviews('OL1W')).single.username, 'liz_bello');
  });
  test(
    'Music reviews receive the public username and retain ownership UUID',
    () async {
      final service = MusicApiService(
        getAccessToken: () async => 'access',
        client: MockClient((request) async {
          if (request.url.path.endsWith('/usernames')) {
            return http.Response(jsonEncode({id: 'liz_bello'}), 200);
          }
          return http.Response(
            jsonEncode([
              {'id': 1, 'userId': id, 'reviewText': 'Reseña'},
            ]),
            200,
          );
        }),
      );
      final reviews = await service.getReviews(
        detail: MusicDetail(
          spotifyId: 'song',
          contentType: 'SONG',
          name: 'Canción',
        ),
      );
      expect(reviews.single['username'], 'liz_bello');
      expect(reviews.single['userId'], id);
    },
  );
}
