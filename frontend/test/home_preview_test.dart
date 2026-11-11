import 'dart:convert';
import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/home/data/services/recommendations_api_service.dart';
import 'package:frontend/features/home/presentation/pages/home_page.dart';
import 'package:frontend/shared/models/musa_destination.dart';
import 'package:frontend/shared/widgets/musa_navigation_shell.dart';

void main() {
  testWidgets(
    'Home contains book recommendations inside the existing navigation',
    (tester) async {
      tester.view.physicalSize = const Size(1366, 900);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      // Optional local fonts make the exported widget-test frame readable (tests use Ahem).
      const previewFont = String.fromEnvironment('MUSA_PREVIEW_FONT');
      const previewIcons = String.fromEnvironment('MUSA_PREVIEW_ICONS');
      if (previewFont.isNotEmpty) {
        await tester.runAsync(() async {
          final bytes = await File(previewFont).readAsBytes();
          for (final family in [
            'Fraunces_600',
            'Manrope_regular',
            'Manrope_600',
            'Manrope_700',
            'Roboto',
          ]) {
            await (FontLoader(
              family,
            )..addFont(Future.value(ByteData.sublistView(bytes)))).load();
          }
          if (previewIcons.isNotEmpty) {
            final icons = await File(previewIcons).readAsBytes();
            await (FontLoader(
              'MaterialIcons',
            )..addFont(Future.value(ByteData.sublistView(icons)))).load();
          }
        });
      }
      final api = RecommendationsApiService(
        getAccessToken: () async => 'preview-token',
        client: MockClient(
          (_) async => http.Response(
            jsonEncode({
              'seed_count': 4,
              'partial': false,
              'items': [
                {
                  'externalId': 'OL1W',
                  'title': 'Catching Fire',
                  'authors': ['Suzanne Collins'],
                  'reason': 'Porque guardaste «The Hunger Games»',
                },
                {
                  'externalId': 'OL2W',
                  'title': 'A Wizard of Earthsea',
                  'authors': ['Ursula K. Le Guin'],
                  'reason': 'Porque guardaste «Harry Potter»',
                },
                {
                  'externalId': 'OL3W',
                  'title': 'The Hobbit',
                  'authors': ['J. R. R. Tolkien'],
                  'reason': 'Porque guardaste «Harry Potter»',
                },
                {
                  'externalId': 'OL4W',
                  'title': 'The Left Hand of Darkness',
                  'authors': ['Ursula K. Le Guin'],
                  'reason': 'Porque guardaste «Dune»',
                },
                {
                  'externalId': 'OL5W',
                  'title': 'The Night Circus',
                  'authors': ['Erin Morgenstern'],
                  'reason': 'Porque guardaste «Harry Potter»',
                },
              ],
            }),
            200,
          ),
        ),
      );
      addTearDown(api.close);
      final boundary = GlobalKey();
      await tester.pumpWidget(
        MaterialApp(
          home: RepaintBoundary(
            key: boundary,
            child: MusaNavigationShell(
              userId: 'preview',
              selectedDestination: MusaDestination.home,
              onDestinationSelected: (_) {},
              child: HomePage(
                userId: 'preview',
                embedded: true,
                recommendationsApi: api,
                onOpenLists: () {},
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Inicio'), findsWidgets);
      expect(find.text('Lecturas para ti'), findsOneWidget);
      expect(find.text('Catching Fire'), findsOneWidget);
      expect(find.text('Todavía no hay contenido'), findsNothing);
      expect(tester.takeException(), isNull);
      const output = String.fromEnvironment('MUSA_PREVIEW_PATH');
      if (output.isNotEmpty) {
        await tester.runAsync(() async {
          final render =
              boundary.currentContext!.findRenderObject()
                  as RenderRepaintBoundary;
          final image = await render.toImage(pixelRatio: 1);
          final png = await image.toByteData(format: ui.ImageByteFormat.png);
          await File(output).writeAsBytes(png!.buffer.asUint8List());
          image.dispose();
        });
      }
    },
  );
}
