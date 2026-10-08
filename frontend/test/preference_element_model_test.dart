import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/features/profile/data/models/preference_element_model.dart';

void main() {
  test(
    'Preferences parse resolved names and images using the list presentation',
    () {
      final element = PreferenceElementModel.fromJson({
        'id': 'preference',
        'elementType': 'BOOK',
        'referenceId': 'OL1W',
        'resolutionStatus': 'AVAILABLE',
        'content': {
          'title': 'Libro',
          'authors': ['Autora'],
          'coverUrl': 'https://example.com/cover.jpg',
        },
      });
      expect(element.isAvailable, isTrue);
      expect(element.title, 'Libro');
      expect(element.subtitle, 'Autora');
      expect(element.imageUrl, 'https://example.com/cover.jpg');
    },
  );
  test('Unavailable preferences retain their reference without an image', () {
    final element = PreferenceElementModel.fromJson({
      'id': 'preference',
      'elementType': 'SONG',
      'referenceId': 'song-id',
      'resolutionStatus': 'UNAVAILABLE',
      'content': null,
    });
    expect(element.referenceId, 'song-id');
    expect(element.imageUrl, isNull);
    expect(element.statusMessage, isNotNull);
  });
}
