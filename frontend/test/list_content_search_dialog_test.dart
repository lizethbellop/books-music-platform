import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'package:frontend/features/books/data/services/books_api_service.dart';
import 'package:frontend/features/music/data/services/music_api_service.dart';
import 'package:frontend/features/profile/presentation/widgets/list_content_search_dialog.dart';

void main() {
  final books = [
    {
      'externalId': 'OL1W',
      'title': 'Libro de prueba',
      'authors': ['Autora'],
      'coverUrl': null,
    },
  ];

  final music = [
    {
      'spotifyId': 'song-test',
      'contentType': 'SONG',
      'name': 'Canción de prueba',
      'artistName': 'Intérprete',
      'imageUrl': null,
    },
    {
      'spotifyId': 'artist-test',
      'contentType': 'ARTIST',
      'name': 'Artista de prueba',
      'imageUrl': null,
    },
    {
      'spotifyId': 'album-test',
      'contentType': 'ALBUM',
      'name': 'Álbum de prueba',
      'imageUrl': null,
    },
  ];

  Future<void> openSearch(
    WidgetTester tester, {
    int booksStatus = 200,
    ValueChanged<ListContentSelection?>? onSelected,
  }) async {
    final booksApi = BooksApiService(
      getAccessToken: () async => 'token-prueba',
      client: MockClient((request) async {
        expect(
          request.headers['Authorization'],
          'Bearer token-prueba',
        );

        return http.Response(
          jsonEncode(books),
          booksStatus,
        );
      }),
    );

    final musicApi = MusicApiService(
      getAccessToken: () async => 'token-prueba',
      client: MockClient((request) async {
        expect(
          request.headers['Authorization'],
          'Bearer token-prueba',
        );

        return http.Response(jsonEncode(music), 200);
      }),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => Scaffold(
            body: ElevatedButton(
              onPressed: () async {
                final selection =
                    await showDialog<ListContentSelection>(
                  context: context,
                  builder: (_) => ListContentSearchDialog(
                    booksApi: booksApi,
                    musicApi: musicApi,
                  ),
                );

                onSelected?.call(selection);
              },
              child: const Text('Abrir buscador'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Abrir buscador'));
    await tester.pumpAndSettle();
  }

  Future<void> search(WidgetTester tester) async {
    await tester.enterText(find.byType(TextField), 'prueba');
    await tester.tap(find.byTooltip('Buscar'));
    await tester.pumpAndSettle();
  }

  testWidgets('Busca libros y devuelve su referencia', (tester) async {
    ListContentSelection? selected;

    await openSearch(
      tester,
      onSelected: (value) => selected = value,
    );

    await search(tester);

    expect(find.text('Libro de prueba'), findsOneWidget);
    expect(find.text('Autora'), findsOneWidget);

    await tester.tap(find.byTooltip('Agregar a la lista'));
    await tester.pumpAndSettle();

    expect(selected?.elementType, 'BOOK');
    expect(selected?.referenceId, 'OL1W');
  });

  testWidgets('Canciones excluye artistas y álbumes', (tester) async {
    ListContentSelection? selected;

    await openSearch(
      tester,
      onSelected: (value) => selected = value,
    );

    await tester.tap(find.text('Canciones'));
    await tester.pumpAndSettle();
    await search(tester);

    expect(find.text('Canción de prueba'), findsOneWidget);
    expect(find.text('Artista de prueba'), findsNothing);
    expect(find.text('Álbum de prueba'), findsNothing);

    await tester.tap(find.byTooltip('Agregar a la lista'));
    await tester.pumpAndSettle();

    expect(selected?.elementType, 'SONG');
    expect(selected?.referenceId, 'song-test');
  });

  testWidgets('Artistas devuelve únicamente artistas', (tester) async {
    ListContentSelection? selected;

    await openSearch(
      tester,
      onSelected: (value) => selected = value,
    );

    await tester.tap(find.text('Artistas'));
    await tester.pumpAndSettle();
    await search(tester);

    expect(find.text('Artista de prueba'), findsOneWidget);
    expect(find.text('Canción de prueba'), findsNothing);
    expect(find.text('Álbum de prueba'), findsNothing);

    await tester.tap(find.byTooltip('Agregar a la lista'));
    await tester.pumpAndSettle();

    expect(selected?.elementType, 'ARTIST');
    expect(selected?.referenceId, 'artist-test');
  });

  testWidgets('Muestra un error cuando falla la búsqueda', (tester) async {
    await openSearch(tester, booksStatus: 500);
    await search(tester);

    expect(
      find.text(
        'No se pudo realizar la búsqueda. Intenta nuevamente.',
      ),
      findsOneWidget,
    );

    expect(find.byTooltip('Agregar a la lista'), findsNothing);
  });
}