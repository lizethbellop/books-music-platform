import 'package:flutter_test/flutter_test.dart';

import 'package:frontend/features/profile/data/models/list_element_model.dart';

void main() {
  Map<String, dynamic> response({
    required String type,
    String status = 'AVAILABLE',
    Map<String, dynamic>? content,
  }) {
    return {
      'id': 'element-test',
      'elementType': type,
      'referenceId': 'reference-test',
      'resolutionStatus': status,
      'content': content,
    };
  }

  test('Libro muestra título, autores y portada', () {
    final element = ListElementModel.fromJson(
      response(
        type: 'BOOK',
        content: {
          'title': 'Libro de prueba',
          'authors': ['Autora uno', 'Autora dos'],
          'coverUrl': 'https://example.com/book.jpg',
        },
      ),
    );

    expect(element.isAvailable, isTrue);
    expect(element.title, 'Libro de prueba');
    expect(element.subtitle, 'Autora uno, Autora dos');
    expect(element.imageUrl, 'https://example.com/book.jpg');
    expect(element.statusMessage, isNull);
  });

  test('Canción muestra nombre, intérprete e imagen', () {
    final element = ListElementModel.fromJson(
      response(
        type: 'SONG',
        content: {
          'name': 'Canción de prueba',
          'artistName': 'Artista de prueba',
          'imageUrl': 'https://example.com/song.jpg',
        },
      ),
    );

    expect(element.title, 'Canción de prueba');
    expect(element.subtitle, 'Artista de prueba');
    expect(element.imageUrl, 'https://example.com/song.jpg');
  });

  test('Artista disponible puede no tener imagen', () {
    final element = ListElementModel.fromJson(
      response(
        type: 'ARTIST',
        content: {
          'name': 'Artista de prueba',
          'imageUrl': null,
        },
      ),
    );

    expect(element.isAvailable, isTrue);
    expect(element.title, 'Artista de prueba');
    expect(element.subtitle, isNull);
    expect(element.imageUrl, isNull);
    expect(element.statusMessage, isNull);
  });

  test('Contenido temporalmente no disponible conserva su referencia', () {
    final element = ListElementModel.fromJson(
      response(
        type: 'BOOK',
        status: 'UNAVAILABLE',
      ),
    );

    expect(element.isAvailable, isFalse);
    expect(element.referenceId, 'reference-test');
    expect(element.title, 'Libro');
    expect(
      element.statusMessage,
      'No se pudo cargar este contenido. Intenta nuevamente.',
    );
  });

  test('Contenido inexistente muestra el mensaje correspondiente', () {
    final element = ListElementModel.fromJson(
      response(
        type: 'SONG',
        status: 'NOT_FOUND',
      ),
    );

    expect(element.isAvailable, isFalse);
    expect(element.title, 'Canción');
    expect(
      element.statusMessage,
      'Este contenido ya no está disponible.',
    );
  });

  test('Respuesta anterior sin contenido sigue siendo compatible', () {
    final element = ListElementModel.fromJson({
      'id': 'element-test',
      'elementType': 'ARTIST',
      'referenceId': 'artist-test',
    });

    expect(element.referenceId, 'artist-test');
    expect(element.resolutionStatus, 'UNAVAILABLE');
    expect(element.content, isNull);
    expect(element.title, 'Artista');
  });
}