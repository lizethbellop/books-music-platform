import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/core/routes/app_shell.dart';
import 'package:frontend/features/auth/data/models/auth_response_model.dart';
import 'package:frontend/features/auth/data/services/auth_session_manager.dart';
import 'package:frontend/features/auth/presentation/screens/login_screen.dart';

void main() {
  const userId = '438b4a66-07c2-42ad-863c-853e0f151901';
  final manager = AuthSessionManager.instance;

  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await manager.clearLocalSession();
  });

  testWidgets(
    'Authenticated app uses the session UUID and returns to login when cleared',
    (tester) async {
      final now = DateTime.now().toUtc();
      await tester.runAsync(
        () => manager.setSession(
          AuthResponseModel(
            accessToken: 'test-access',
            refreshToken: 'test-refresh',
            userId: userId,
            username: 'prueba',
            fullName: 'Prueba',
            roleName: 'USUARIO',
            accessTokenExpiresAt: now.add(const Duration(minutes: 15)),
            sessionExpiresAt: now.add(const Duration(days: 7)),
          ),
          rememberMe: false,
        ),
      );
      await tester.pumpWidget(const MusaApp());
      await tester.pump();
      expect(tester.widget<AppShell>(find.byType(AppShell)).userId, userId);
      await tester.runAsync(manager.clearLocalSession);
      await tester.pumpAndSettle();
      expect(find.byType(LoginScreen), findsOneWidget);
      expect(find.byType(AppShell), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}
