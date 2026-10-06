import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';

void main() {
  testWidgets('Musa app loads', (WidgetTester tester) async {
    await tester.pumpWidget(const MusaApp());

    expect(find.text('musa.'), findsOneWidget);
  });
}