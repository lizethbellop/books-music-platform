import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/core/config/api_config.dart';
import 'package:frontend/features/books/presentation/pages/book_detail_page.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';
import 'package:frontend/features/home/presentation/widgets/home_book_recommendations_section.dart';

Map<String, dynamic> response({int seeds = 2, bool empty = false}) => {
  'seed_count': seeds,
  'partial': false,
  'items': empty
      ? []
      : [
          {
            'externalId': 'OL777W',
            'title': 'La próxima lectura',
            'authors': ['Una autora'],
            'coverUrl': null,
            'firstPublishYear': 2024,
            'reason': 'Porque guardaste «Un libro»',
          },
        ],
};

void main() {
  test('Home service uses the Gateway and current bearer without userId or manual seeds', () async {
    final api = RecommendationsApiService(
      getAccessToken: () async => 'session-token',
      client: MockClient((request) async {
        expect(request.url.origin, ApiConfig.gatewayUrl);
        expect(request.url.path, '/api/recommendations/books/me');
        expect(request.url.queryParameters, isEmpty);
        expect(request.headers['Authorization'], 'Bearer session-token');
        return http.Response(jsonEncode(response()), 200);
      }),
    );
    addTearDown(api.close);
    final result = await api.getHomeBooks();
    expect(result.seedCount, 2);
    expect(result.items.single.book.externalId, 'OL777W');
  });

  for (final width in [320.0, 390.0, 1366.0]) {
    testWidgets('Recommendations live in Home and fit width $width', (
      tester,
    ) async {
      tester.view.physicalSize = Size(width, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      var openedLists = false;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'token',
        client: MockClient(
          (_) async => http.Response(jsonEncode(response()), 200),
        ),
      );
      addTearDown(api.close);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: HomeBookRecommendationsSection(
                userId: 'owner',
                api: api,
                onOpenLists: () => openedLists = true,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Lecturas para ti'), findsOneWidget);
      expect(find.text('La próxima lectura'), findsOneWidget);
      expect(find.textContaining('Porque guardaste'), findsOneWidget);
      await tester.tap(find.text('Mis listas'));
      expect(openedLists, isTrue);
      expect(tester.takeException(), isNull);
      await tester.tap(find.text('La próxima lectura'));
      await tester.pumpAndSettle();
      expect(find.byType(BookDetailPage), findsOneWidget);
      final detail = tester.widget<BookDetailPage>(find.byType(BookDetailPage));
      expect(detail.externalId, 'OL777W');
      expect(detail.userId, 'owner');
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets(
    'A source failure offers retry instead of fabricated suggestions',
    (tester) async {
      var calls = 0;
      final api = RecommendationsApiService(
        getAccessToken: () async => 'token',
        client: MockClient((_) async {
          calls++;
          return calls == 1
              ? http.Response('{}', 503)
              : http.Response(jsonEncode(response()), 200);
        }),
      );
      addTearDown(api.close);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: HomeBookRecommendationsSection(
              userId: 'owner',
              api: api,
              onOpenLists: () {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Reintentar'), findsOneWidget);
      expect(find.text('La próxima lectura'), findsNothing);
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();
      expect(find.text('La próxima lectura'), findsOneWidget);
    },
  );

  testWidgets(
    'Empty lists explain how to personalize without asking for ratings',
    (tester) async {
      final api = RecommendationsApiService(
        getAccessToken: () async => 'token',
        client: MockClient(
          (_) async => http.Response(jsonEncode(response(seeds: 0)), 200),
        ),
      );
      addTearDown(api.close);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: HomeBookRecommendationsSection(
              userId: 'owner',
              api: api,
              onOpenLists: () {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(
        find.textContaining('Agrega libros a tus preferencias o listas'),
        findsOneWidget,
      );
    },
  );
}
