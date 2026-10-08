import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/features/profile/data/models/list_element_model.dart';
import 'package:frontend/features/profile/presentation/widgets/collection_poster_strip.dart';

void main() {
  final elements = [
    const ListElementModel(
      id: 'book',
      elementType: 'BOOK',
      referenceId: 'OL1W',
      resolutionStatus: 'AVAILABLE',
      content: {
        'title':
            'Un libro con un nombre muy largo para comprobar la distribución',
        'authors': ['Autora'],
      },
    ),
    const ListElementModel(
      id: 'song',
      elementType: 'SONG',
      referenceId: 'song',
      resolutionStatus: 'AVAILABLE',
      content: {'name': 'Canción', 'artistName': 'Artista'},
    ),
    const ListElementModel(
      id: 'artist',
      elementType: 'ARTIST',
      referenceId: 'artist',
      resolutionStatus: 'UNAVAILABLE',
    ),
  ];
  for (final width in [320.0, 390.0, 1366.0]) {
    testWidgets('Poster row fits width $width with working removal', (
      tester,
    ) async {
      tester.view.physicalSize = Size(width, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      ListElementModel? removed;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CollectionPosterStrip(
              elements: elements,
              onRemove: (element) => removed = element,
            ),
          ),
        ),
      );
      await tester.pump();
      expect(find.textContaining('Un libro'), findsOneWidget);
      expect(find.text('Autora'), findsOneWidget);
      await tester.tap(find.byTooltip('Quitar de la lista').first);
      expect(removed?.id, 'book');
      expect(tester.takeException(), isNull);
    });
  }
}
