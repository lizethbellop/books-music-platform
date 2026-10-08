import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/core/routes/app_shell.dart';
import 'package:frontend/core/theme/app_colors.dart';
import 'package:frontend/shared/widgets/musa_sidebar.dart';
import 'package:frontend/shared/widgets/musa_bottom_navigation.dart';

void main() {
  testWidgets('Desktop has exactly one light sidebar and reachable sections', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1366, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(const MaterialApp(home: AppShell()));
    await tester.pump();
    expect(find.byType(MusaSidebar), findsOneWidget);
    final container = tester.widget<Container>(
      find
          .descendant(
            of: find.byType(MusaSidebar),
            matching: find.byType(Container),
          )
          .first,
    );
    expect((container.decoration as BoxDecoration).color, AppColors.warmWhite);
    for (final name in [
      'Música',
      'Libros',
      'Mi perfil',
      'Social',
      'Comunidades',
      'Estadísticas',
      'Inicio',
    ]) {
      await tester.tap(
        find.descendant(
          of: find.byType(MusaSidebar),
          matching: name == 'Mi perfil'
              ? find.byTooltip('Mi perfil')
              : find.text(name),
        ),
      );
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));
      expect(find.byType(MusaSidebar), findsOneWidget, reason: name);
      expect(tester.takeException(), isNull, reason: name);
    }
  });

  testWidgets(
    'Mobile uses only bottom navigation without a top bar or drawer',
    (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(const MaterialApp(home: AppShell()));
      await tester.pump();
      expect(find.byType(MusaBottomNavigation), findsOneWidget);
      expect(find.byType(MusaSidebar), findsNothing);
      for (final name in ['Explorar', 'Música', 'Libros', 'Perfil', 'Inicio']) {
        await tester.tap(
          find.descendant(
            of: find.byType(MusaBottomNavigation),
            matching: find.text(name),
          ),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 400));
        expect(tester.takeException(), isNull, reason: name);
      }
      expect(find.byType(AppBar), findsNothing);
      expect(find.byType(Drawer), findsNothing);
      expect(find.byIcon(Icons.menu), findsNothing);
      expect(find.byType(MusaSidebar), findsNothing);
      expect(find.byType(MusaBottomNavigation), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
