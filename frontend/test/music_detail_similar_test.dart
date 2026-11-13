import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/music/data/models/music_search_item.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/music/presentation/pages/music_detail_page.dart';
import 'package:frontend/features/home/data/models/home_music_recommendations.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';

void main() {
  for (final width in [390.0, 1366.0]) {
    for (final kind in MusicRecommendationKind.values) {
      testWidgets(
        '${kind.name} detail has reachable similar recommendations at $width',
        (tester) async {
          tester.view.physicalSize = Size(width, 900);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);
          final music = MusicApiService(
            getAccessToken: () async => 'session',
            client: MockClient((request) async {
              final path = request.url.path;
              if (path.contains('/ratings/user/')) {
                return http.Response('0', 200);
              }
              if (path.contains('/favorites/user/')) {
                return http.Response('false', 200);
              }
              if (path.contains('/reviews/user/')) {
                return http.Response('null', 200);
              }
              if (path.contains('/reviews/content')) {
                return http.Response('[]', 200);
              }
              return http.Response(
                jsonEncode({
                  'spotifyId': 'reference',
                  'contentType': kind.contentType,
                  'name': 'Reference music',
                }),
                200,
              );
            }),
          );
          var calls = 0;
          final recommendations = RecommendationsApiService(
            getAccessToken: () async => 'session',
            client: MockClient((request) async {
              if (request.url.path.startsWith('/api/music/')) {
                return http.Response('{}', 200);
              }
              calls++;
              expect(
                request.url.path,
                '/api/recommendations/music/${kind.name}/reference/similar',
              );
              expect(request.headers['Authorization'], 'Bearer session');
              return http.Response(
                jsonEncode({
                  'seed_count': 1,
                  'mode': 'knn_content',
                  'items': [
                    {
                      'spotifyId': 'neighbor',
                      'contentType': kind.contentType,
                      'name': 'Similar music',
                      'artistNames': ['Example'],
                      'reason': 'Shared style',
                    },
                  ],
                }),
                200,
              );
            }),
          );
          addTearDown(music.close);
          addTearDown(recommendations.close);
          await tester.pumpWidget(
            MaterialApp(
              home: MusicDetailPage(
                userId: 'owner',
                item: MusicSearchItem(
                  spotifyId: 'reference',
                  contentType: kind.contentType,
                  name: 'Reference music',
                ),
                musicApi: music,
                recommendationsApi: recommendations,
              ),
            ),
          );
          await tester.pumpAndSettle();
          expect(calls, 1);
          final title = switch (kind) {
            MusicRecommendationKind.artists => 'Artistas similares',
            MusicRecommendationKind.albums => 'Álbumes similares',
            MusicRecommendationKind.songs => 'Canciones similares',
          };
          await tester.drag(
            find.byType(SingleChildScrollView).first,
            const Offset(0, -2000),
          );
          await tester.pumpAndSettle();
          expect(find.text(title), findsOneWidget);
          expect(find.text('Similar music'), findsOneWidget);
          expect(tester.getRect(find.text(title)).top, greaterThanOrEqualTo(0));
          await tester.pump(const Duration(minutes: 3));
          expect(calls, 1);
          expect(
            find.byTooltip('Actualizar recomendaciones musicales'),
            findsNothing,
          );
          expect(find.text('Actualizar'), findsNothing);
          await tester.tap(find.text('Similar music'));
          await tester.pumpAndSettle();
          final opened = tester.widget<MusicDetailPage>(
            find.byType(MusicDetailPage),
          );
          expect(opened.item.spotifyId, 'neighbor');
          expect(opened.item.contentType, kind.contentType);
          await tester.pageBack();
          await tester.pumpAndSettle();
          expect(calls, 1);
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
}
