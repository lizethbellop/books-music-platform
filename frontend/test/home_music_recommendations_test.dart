import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/home/data/models/home_music_recommendations.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';
import 'package:frontend/features/home/presentation/widgets/home_music_recommendations_section.dart';
import 'package:frontend/features/music/presentation/pages/music_detail_page.dart';

Map<String, dynamic> musicResponse(
  MusicRecommendationKind kind, {
  bool empty = false,
  String mode = 'knn_content',
}) => {
  'seed_count': 2,
  'mode': mode,
  'partial': false,
  'items': empty
      ? []
      : [
          {
            'spotifyId': 'verified-item',
            'contentType': kind.contentType,
            'name': 'Sugerencia musical',
            'artistNames': ['Artista de ejemplo'],
            'reason': 'Comparte indie pop con una de tus preferencias',
          },
        ],
};

void main() {
  for (final kind in MusicRecommendationKind.values) {
    test('Uses the authenticated ${kind.name} endpoint', () async {
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient((request) async {
          expect(
            request.url.path,
            '/api/recommendations/music/${kind.name}/me',
          );
          expect(request.headers['Authorization'], 'Bearer session');
          expect(request.url.queryParameters.containsKey('userId'), isFalse);
          return http.Response(jsonEncode(musicResponse(kind)), 200);
        }),
      );
      addTearDown(api.close);
      expect(
        (await api.getHomeMusic(kind)).items.single.item.contentType,
        kind.contentType,
      );
    });
  }

  for (final kind in [
    MusicRecommendationKind.artists,
    MusicRecommendationKind.songs,
  ]) {
    test('Loads and reuses the authenticated ${kind.name} image', () async {
      var calls = 0;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient((request) async {
          calls++;
          expect(
            request.url.path,
            '/api/music/${kind.contentType}/verified-item',
          );
          expect(request.headers['Authorization'], 'Bearer session');
          return http.Response(
            jsonEncode({'imageUrl': 'https://i.scdn.co/image/example'}),
            200,
          );
        }),
      );
      addTearDown(api.close);
      final recommendation = HomeMusicRecommendations.fromJson(
        musicResponse(kind),
      ).items.single;
      final images = await Future.wait([
        api.getMusicImage(recommendation),
        api.getMusicImage(recommendation),
      ]);
      expect(images, everyElement('https://i.scdn.co/image/example'));
      expect(await api.getMusicImage(recommendation), images.first);
      expect(calls, 1);
    });
  }

  test(
    'Image failure keeps the recommendation usable and allows retry',
    () async {
      var calls = 0;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient(
          (_) async => ++calls == 1
              ? http.Response('{}', 503)
              : http.Response(
                  jsonEncode({'imageUrl': 'https://i.scdn.co/image/recovered'}),
                  200,
                ),
        ),
      );
      addTearDown(api.close);
      final recommendation = HomeMusicRecommendations.fromJson(
        musicResponse(MusicRecommendationKind.songs),
      ).items.single;
      expect(await api.getMusicImage(recommendation), isNull);
      expect(await api.getMusicImage(recommendation), isNull);
      expect(calls, 1);
      api.retryMissingMusicImages();
      expect(
        await api.getMusicImage(recommendation),
        'https://i.scdn.co/image/recovered',
      );
      expect(recommendation.item.spotifyId, 'verified-item');
    },
  );

  test('Existing cover needs no music detail request', () async {
    final api = RecommendationsApiService(
      client: MockClient((_) async => throw StateError('Unexpected request')),
    );
    addTearDown(api.close);
    final recommendation = RecommendedMusic.fromJson({
      'spotifyId': 'album',
      'contentType': 'ALBUM',
      'name': 'Album',
      'imageUrl': 'https://example.com/cover.jpg',
    });
    expect(
      await api.getMusicImage(recommendation),
      'https://example.com/cover.jpg',
    );
  });

  testWidgets('Pending music refreshes only when requested', (tester) async {
    var calls = 0;
    final api = RecommendationsApiService(
      getAccessToken: () async => 'session',
      client: MockClient((request) async {
        if (request.url.path.startsWith('/api/music/')) {
          return http.Response('{}', 200);
        }
        calls++;
        return http.Response(
          jsonEncode({
            ...musicResponse(MusicRecommendationKind.songs, empty: calls == 1),
            'preparation': {'status': calls == 1 ? 'pending' : 'ready'},
          }),
          200,
        );
      }),
    );
    addTearDown(api.close);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: HomeMusicRecommendationsSection(
              userId: 'owner',
              kind: MusicRecommendationKind.songs,
              api: api,
              onOpenLists: () {},
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(
      find.textContaining('Estamos preparando más opciones musicales'),
      findsOneWidget,
    );
    await tester.pump(const Duration(minutes: 3));
    expect(calls, 1);
    await tester.tap(find.byTooltip('Actualizar recomendaciones musicales'));
    await tester.pumpAndSettle();
    expect(calls, 2);
    expect(find.text('Sugerencia musical'), findsOneWidget);
    expect(
      find.textContaining('Estamos preparando más opciones musicales'),
      findsNothing,
    );
    await tester.pump(const Duration(seconds: 20));
    expect(calls, 2);
  });

  testWidgets('Leaving pending music never triggers another request', (
    tester,
  ) async {
    var calls = 0;
    final api = RecommendationsApiService(
      getAccessToken: () async => 'session',
      client: MockClient((_) async {
        calls++;
        return http.Response(
          jsonEncode({
            ...musicResponse(MusicRecommendationKind.songs, empty: true),
            'preparation': {'status': 'pending'},
          }),
          200,
        );
      }),
    );
    addTearDown(api.close);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeMusicRecommendationsSection(
            userId: 'owner',
            kind: MusicRecommendationKind.songs,
            api: api,
            onOpenLists: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.pumpWidget(const SizedBox());
    await tester.pump(const Duration(seconds: 20));
    expect(calls, 1);
    expect(tester.takeException(), isNull);
  });

  for (final width in [320.0, 390.0, 1366.0]) {
    for (final kind in MusicRecommendationKind.values) {
      testWidgets(
        '${kind.name} carousel fits $width and opens the right detail',
        (tester) async {
          tester.view.physicalSize = Size(width, 844);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);
          final api = RecommendationsApiService(
            getAccessToken: () async => 'session',
            client: MockClient(
              (_) async => http.Response(jsonEncode(musicResponse(kind)), 200),
            ),
          );
          addTearDown(api.close);
          await tester.pumpWidget(
            MaterialApp(
              home: Scaffold(
                body: SingleChildScrollView(
                  child: HomeMusicRecommendationsSection(
                    userId: 'owner',
                    kind: kind,
                    api: api,
                    onOpenLists: () {},
                  ),
                ),
              ),
            ),
          );
          await tester.pumpAndSettle();
          expect(find.text('Sugerencia musical'), findsOneWidget);
          expect(tester.takeException(), isNull);
          await tester.tap(find.text('Sugerencia musical'));
          await tester.pumpAndSettle();
          final detail = tester.widget<MusicDetailPage>(
            find.byType(MusicDetailPage),
          );
          expect(detail.item.contentType, kind.contentType);
          expect(detail.item.spotifyId, 'verified-item');
          expect(detail.userId, 'owner');
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  test(
    'Rejects a different content type instead of opening the wrong detail',
    () async {
      final api = RecommendationsApiService(
        getAccessToken: () async => 'session',
        client: MockClient(
          (_) async => http.Response(
            jsonEncode(musicResponse(MusicRecommendationKind.songs)),
            200,
          ),
        ),
      );
      addTearDown(api.close);
      await expectLater(
        api.getHomeMusic(MusicRecommendationKind.albums),
        throwsA(isA<RecommendationsApiException>()),
      );
    },
  );

  testWidgets('A source error can be retried', (tester) async {
    var calls = 0;
    final api = RecommendationsApiService(
      getAccessToken: () async => 'session',
      client: MockClient(
        (_) async => ++calls == 1
            ? http.Response('{}', 503)
            : http.Response(
                jsonEncode(musicResponse(MusicRecommendationKind.artists)),
                200,
              ),
      ),
    );
    addTearDown(api.close);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: HomeMusicRecommendationsSection(
              userId: 'owner',
              kind: MusicRecommendationKind.artists,
              api: api,
              onOpenLists: () {},
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Reintentar'), findsOneWidget);
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();
    expect(find.text('Sugerencia musical'), findsOneWidget);
  });
}
