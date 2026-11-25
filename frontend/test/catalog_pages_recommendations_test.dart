import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/music/presentation/pages/music_home_page.dart';
import 'package:frontend/features/books/presentation/pages/books_home_page.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';

void main() {
  testWidgets(
    'Music All and category chips show personalized results without refetching',
    (tester) async {
      final requests = <String>[];
      var imageRequests = 0;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient((request) async {
          if (request.url.path.startsWith('/api/music/')) {
            imageRequests++;
            return http.Response('{}', 200);
          }
          requests.add(request.url.path);
          final kind = request.url.path.split('/')[4];
          final contentType = {
            'artists': 'ARTIST',
            'albums': 'ALBUM',
            'songs': 'SONG',
          }[kind]!;
          return http.Response(
            jsonEncode({
              'seed_count': 1,
              'mode': 'knn_content',
              'items': [
                {
                  'spotifyId': kind,
                  'contentType': contentType,
                  'name': 'Personal $kind',
                  'artistNames': ['Artist'],
                  'reason': 'From your activity',
                },
              ],
            }),
            200,
          );
        }),
      );
      final music = MusicApiService(
        getAccessToken: () async => 'session',
        client: MockClient((_) async => http.Response('[]', 200)),
      );
      addTearDown(api.close);
      addTearDown(music.close);
      await tester.pumpWidget(
        MaterialApp(
          home: MusicHomePage(
            userId: 'owner',
            recommendationsApi: api,
            musicApi: music,
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(requests.length, 3);
      final initialImageRequests = imageRequests;
      for (final kind in ['artists', 'albums', 'songs']) {
        expect(find.text('Personal $kind'), findsOneWidget);
      }
      expect(
        find.byTooltip('Actualizar recomendaciones musicales'),
        findsNothing,
      );
      for (final entry in {
        'Artistas': 'artists',
        'Álbumes': 'albums',
        'Canciones': 'songs',
      }.entries) {
        await tester.tap(find.text(entry.key));
        await tester.pumpAndSettle();
        expect(find.text('Personal ${entry.value}'), findsOneWidget);
        for (final other in [
          'artists',
          'albums',
          'songs',
        ].where((kind) => kind != entry.value)) {
          expect(find.text('Personal $other'), findsNothing);
        }
        expect(requests.length, 3);
        expect(imageRequests, initialImageRequests);
      }
      await tester.tap(find.text('Todo'));
      await tester.pumpAndSettle();
      expect(requests.length, 3);
      expect(find.text('Personal albums'), findsOneWidget);
      await tester.pump(const Duration(minutes: 3));
      expect(requests.length, 3);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Books default screen uses the personalized endpoint, not generic exploration',
    (tester) async {
      var calls = 0;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient((request) async {
          calls++;
          expect(request.url.path, '/api/recommendations/books/knn/me');
          return http.Response(
            jsonEncode({
              'seed_count': 1,
              'mode': 'knn_content',
              'items': [
                {
                  'externalId': 'OL123W',
                  'title': 'Personal book',
                  'authors': ['Author'],
                  'reason': 'Because of your favorites',
                },
              ],
            }),
            200,
          );
        }),
      );
      addTearDown(api.close);
      await tester.pumpWidget(
        MaterialApp(
          home: BooksHomePage(userId: 'owner', recommendationsApi: api),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Personal book'), findsOneWidget);
      expect(find.text('Explorar libros'), findsNothing);
      expect(find.byTooltip('Actualizar recomendaciones'), findsNothing);
      await tester.pump(const Duration(minutes: 3));
      expect(calls, 1);
      expect(tester.takeException(), isNull);
    },
  );
}
