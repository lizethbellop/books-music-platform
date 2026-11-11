import 'dart:convert';
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/core/routes/app_routes.dart';
import 'package:frontend/features/books/data/services/books_api_service.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/onboarding/data/preferences_onboarding_api.dart';
import 'package:frontend/features/onboarding/presentation/preferences_onboarding_screen.dart';

const books = [
  {
    'externalId': 'OL1W',
    'title': 'Un libro',
    'authors': ['Una autora'],
  },
];
const music = [
  {
    'spotifyId': 'song-1',
    'contentType': 'SONG',
    'name': 'Una canción',
    'artistName': 'Un artista',
  },
  {'spotifyId': 'artist-1', 'contentType': 'ARTIST', 'name': 'Un artista'},
  {'spotifyId': 'album-1', 'contentType': 'ALBUM', 'name': 'Un álbum'},
];

Widget screen({
  required PreferencesOnboardingApi api,
  required BooksApiService booksApi,
  required MusicApiService musicApi,
}) => MaterialApp(
  initialRoute: '/test-onboarding',
  routes: {
    AppRoutes.home: (_) => const Scaffold(body: Text('Inicio preparado')),
    '/test-onboarding': (_) => PreferencesOnboardingScreen(
      userId: 'owner',
      booksApi: booksApi,
      musicApi: musicApi,
      preferencesApi: api,
    ),
  },
);

void main() {
  test('Saves selected types in a single authenticated batch, without a client userId', () async {
    final calls = <String>[];
    final api = PreferencesOnboardingApi(
      getAccessToken: () async => 'current-token',
      client: MockClient((r) async {
        calls.add(r.url.path);
        expect(r.headers['Authorization'], 'Bearer current-token');
        expect(r.url.queryParameters, isEmpty);
        if (r.url.path.endsWith('/batch')) {
          expect(jsonDecode(r.body), {
            'elements': [
              {'elementType': 'BOOK', 'referenceId': 'OL1W'},
            ],
          });
        }
        return http.Response('{}', 200);
      }),
    );
    addTearDown(api.close);
    await api.ensureProfile();
    await api.save([
      {'elementType': 'BOOK', 'referenceId': 'OL1W'},
    ]);
    expect(calls, ['/api/profiles/me', '/api/profiles/me/preferences/batch']);
  });

  for (final width in [320.0, 1366.0]) {
    testWidgets(
      'Selects across books, songs and artists then saves together at $width',
      (tester) async {
        tester.view.physicalSize = Size(width, 1000);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final saved = <dynamic>[];
        final api = PreferencesOnboardingApi(
          getAccessToken: () async => 'token',
          client: MockClient((r) async {
            if (r.url.path.endsWith('/batch')) saved.add(jsonDecode(r.body));
            return http.Response('{}', 200);
          }),
        );
        final bookApi = BooksApiService(
          getAccessToken: () async => 'token',
          client: MockClient(
            (_) async => http.Response(jsonEncode(books), 200),
          ),
        );
        final musicApi = MusicApiService(
          getAccessToken: () async => 'token',
          client: MockClient(
            (_) async => http.Response(jsonEncode(music), 200),
          ),
        );
        addTearDown(api.close);
        addTearDown(bookApi.close);
        addTearDown(musicApi.close);
        await tester.pumpWidget(
          screen(api: api, booksApi: bookApi, musicApi: musicApi),
        );
        await tester.pumpAndSettle();
        await tester.enterText(find.byType(TextField), 'consulta');
        await tester.pump(const Duration(milliseconds: 450));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const ValueKey('BOOK:OL1W')));
        await tester.pump();
        await tester.scrollUntilVisible(
          find.text('Canciones'),
          -150,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.tap(find.text('Canciones'));
        await tester.pump(const Duration(milliseconds: 450));
        await tester.pumpAndSettle();
        expect(find.text('Un álbum'), findsNothing);
        expect(find.byKey(const ValueKey('ARTIST:artist-1')), findsNothing);
        await tester.tap(find.byKey(const ValueKey('SONG:song-1')));
        await tester.pump();
        await tester.scrollUntilVisible(
          find.text('Artistas'),
          -150,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.tap(find.text('Artistas'));
        await tester.pump(const Duration(milliseconds: 450));
        await tester.pumpAndSettle();
        await tester.scrollUntilVisible(
          find.byKey(const ValueKey('ARTIST:artist-1')),
          150,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.tap(find.byKey(const ValueKey('ARTIST:artist-1')));
        await tester.pump();
        tester.testTextInput.hide();
        await tester.pumpAndSettle();
        await tester.ensureVisible(find.text('Guardar y continuar'));
        await tester.tap(find.text('Guardar y continuar'));
        await tester.pumpAndSettle();
        expect(find.text('Inicio preparado'), findsOneWidget);
        expect((saved.single as Map)['elements'], [
          {'elementType': 'BOOK', 'referenceId': 'OL1W'},
          {'elementType': 'SONG', 'referenceId': 'song-1'},
          {'elementType': 'ARTIST', 'referenceId': 'artist-1'},
        ]);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('Failed save retains the selection and retries the same batch', (
    tester,
  ) async {
    var saves = 0;
    final api = PreferencesOnboardingApi(
      getAccessToken: () async => 'token',
      client: MockClient((r) async {
        if (r.url.path.endsWith('/batch') && ++saves == 1) {
          return http.Response('{}', 503);
        }
        return http.Response('{}', 200);
      }),
    );
    final bookApi = BooksApiService(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response(jsonEncode(books), 200)),
    );
    final musicApi = MusicApiService(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response('[]', 200)),
    );
    addTearDown(api.close);
    addTearDown(bookApi.close);
    addTearDown(musicApi.close);
    await tester.pumpWidget(
      screen(api: api, booksApi: bookApi, musicApi: musicApi),
    );
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'libro');
    await tester.pump(const Duration(milliseconds: 450));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('BOOK:OL1W')));
    await tester.pump();
    tester.testTextInput.hide();
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Guardar y continuar'));
    await tester.tap(find.text('Guardar y continuar'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Tu selección sigue aquí'), findsOneWidget);
    expect(find.text('Inicio preparado'), findsNothing);
    tester.testTextInput.hide();
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Guardar y continuar'));
    await tester.tap(find.text('Guardar y continuar'));
    await tester.pumpAndSettle();
    expect(saves, 2);
    expect(find.text('Inicio preparado'), findsOneWidget);
  });

  testWidgets('Skip continues without writing preferences', (tester) async {
    final api = PreferencesOnboardingApi(
      getAccessToken: () async => 'token',
      client: MockClient((r) async {
        expect(r.url.path.endsWith('/batch'), isFalse);
        return http.Response('{}', 200);
      }),
    );
    final bookApi = BooksApiService(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response('[]', 200)),
    );
    final musicApi = MusicApiService(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response('[]', 200)),
    );
    addTearDown(api.close);
    addTearDown(bookApi.close);
    addTearDown(musicApi.close);
    await tester.pumpWidget(
      screen(api: api, booksApi: bookApi, musicApi: musicApi),
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Omitir por ahora'));
    await tester.tap(find.text('Omitir por ahora'));
    await tester.pumpAndSettle();
    expect(find.text('Inicio preparado'), findsOneWidget);
  });

  testWidgets('An older search cannot replace newer results', (tester) async {
    final old = Completer<http.Response>();
    final api = PreferencesOnboardingApi(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response('{}', 200)),
    );
    final bookApi = BooksApiService(
      getAccessToken: () async => 'token',
      client: MockClient((r) async {
        if (r.url.queryParameters['q'] == 'old') return old.future;
        return http.Response(
          jsonEncode([
            {'externalId': 'OL2W', 'title': 'Resultado nuevo', 'authors': []},
          ]),
          200,
        );
      }),
    );
    final musicApi = MusicApiService(
      getAccessToken: () async => 'token',
      client: MockClient((_) async => http.Response('[]', 200)),
    );
    addTearDown(api.close);
    addTearDown(bookApi.close);
    addTearDown(musicApi.close);
    await tester.pumpWidget(
      screen(api: api, booksApi: bookApi, musicApi: musicApi),
    );
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'old');
    await tester.pump(const Duration(milliseconds: 450));
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'new');
    await tester.pump(const Duration(milliseconds: 450));
    await tester.pumpAndSettle();
    old.complete(http.Response(jsonEncode(books), 200));
    await tester.pumpAndSettle();
    expect(find.text('Resultado nuevo'), findsOneWidget);
    expect(find.byKey(const ValueKey('BOOK:OL1W')), findsNothing);
  });
}
