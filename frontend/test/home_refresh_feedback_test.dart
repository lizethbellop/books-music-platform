import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';
import 'package:frontend/features/home/data/models/home_music_recommendations.dart';
import 'package:frontend/features/home/presentation/widgets/home_book_recommendations_section.dart';
import 'package:frontend/features/home/presentation/widgets/home_music_recommendations_section.dart';

void main() {
  for (final category in ['books', 'artists', 'albums', 'songs']) {
    testWidgets(
      'Home $category refresh requests fresh data and reports unchanged results',
      (tester) async {
        final pending = Completer<http.Response>();
        var calls = 0;
        final kind = category == 'books'
            ? null
            : MusicRecommendationKind.values.byName(category);
        Map<String, dynamic> payload(bool changed) => {
          'seed_count': 1,
          'mode': 'knn_content',
          'preparation': {'status': 'ready'},
          'items': [
            if (kind == null)
              {
                'externalId': changed ? 'OL2W' : 'OL1W',
                'title': changed ? 'Updated result' : 'Initial result',
                'authors': ['Author'],
                'reason': 'Shared features',
              }
            else
              {
                'spotifyId': changed ? 'updated' : 'initial',
                'contentType': kind.contentType,
                'name': changed ? 'Updated result' : 'Initial result',
                'artistNames': ['Artist'],
                'reason': 'Shared features',
              },
          ],
        };
        final api = RecommendationsApiService(
          getAccessToken: () async => 'session',
          client: MockClient((request) async {
            if (request.url.path.startsWith('/api/music/')) {
              return http.Response('{}', 200);
            }
            calls++;
            expect(request.headers['Authorization'], 'Bearer session');
            if (calls == 2) return pending.future;
            return http.Response(jsonEncode(payload(calls > 1)), 200);
          }),
        );
        addTearDown(api.close);
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: SingleChildScrollView(
                child: kind == null
                    ? HomeBookRecommendationsSection(
                        userId: 'owner',
                        api: api,
                        onOpenLists: () {},
                      )
                    : HomeMusicRecommendationsSection(
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
        expect(find.text('Initial result'), findsOneWidget);
        final tooltip = kind == null
            ? 'Actualizar recomendaciones'
            : 'Actualizar recomendaciones musicales';
        final busy = kind == null
            ? 'Actualizando recomendaciones'
            : 'Actualizando recomendaciones musicales';
        await tester.tap(find.byTooltip(tooltip));
        await tester.pump();
        expect(calls, 2);
        expect(
          tester
              .widget<IconButton>(
                find.byWidgetPredicate(
                  (widget) => widget is IconButton && widget.tooltip == busy,
                ),
              )
              .onPressed,
          isNull,
        );
        pending.complete(http.Response(jsonEncode(payload(true)), 200));
        await tester.pumpAndSettle();
        expect(find.text('Updated result'), findsOneWidget);
        expect(find.text('Initial result'), findsNothing);
        expect(find.text('Recomendaciones actualizadas.'), findsOneWidget);
        await tester.tap(find.byTooltip(tooltip));
        await tester.pumpAndSettle();
        expect(calls, 3);
        expect(
          find.text('Actualizado. No hay nuevas sugerencias por ahora.'),
          findsOneWidget,
        );
        await tester.pump(const Duration(minutes: 3));
        expect(calls, 3);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
