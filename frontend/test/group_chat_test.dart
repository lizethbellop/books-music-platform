import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:frontend/features/chat/data/chat_api.dart';
import 'package:frontend/features/chat/data/chat_models.dart';
import 'package:frontend/features/chat/presentation/create_group_page.dart';
import 'package:frontend/features/chat/presentation/group_members_page.dart';
import 'package:frontend/features/auth/data/models/auth_response_model.dart';
import 'package:frontend/features/auth/data/services/auth_session_manager.dart';

void main() {
  Map<String, dynamic> group({String owner = 'liz'}) => {
    'id': 'g',
    'type': 'GROUP',
    'name': 'Lecturas',
    'ownerId': owner,
    'participantIds': ['liz', 'ana'],
  };
  test('models preserve group metadata and private compatibility', () {
    final g = ChatConversation.fromJson(group());
    expect(g.isGroup, isTrue);
    expect(g.ownerId, 'liz');
    expect(g.name, 'Lecturas');
    expect(
      ChatConversation.fromJson({
        'id': 'p',
        'participantIds': ['a', 'b'],
      }).isGroup,
      isFalse,
    );
  });
  test(
    'API creates groups without accepting a client supplied owner',
    () async {
      final api = ChatApi(
        client: MockClient((request) async {
          expect(request.url.path.endsWith('/conversations/groups'), isTrue);
          expect(jsonDecode(request.body), {
            'name': 'Lecturas',
            'participantIds': ['ana'],
          });
          return http.Response(jsonEncode(group()), 200);
        }),
      );
      expect((await api.createGroup('Lecturas', ['ana'])).isGroup, isTrue);
      api.close();
    },
  );
  test('leave handles 204 and removal uses the membership resource', () async {
    final api = ChatApi(
      client: MockClient((request) async {
        if (request.url.path.endsWith('/leave')) return http.Response('', 204);
        expect(request.method, 'DELETE');
        expect(request.url.path.endsWith('/g/participants/ana'), isTrue);
        return http.Response(jsonEncode(group()), 200);
      }),
    );
    await api.leaveGroup('g');
    await api.removeParticipant('g', 'ana');
    api.close();
  });
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    final now = DateTime.now().toUtc();
    await AuthSessionManager.instance.setSession(
      AuthResponseModel(
        accessToken: 'test',
        refreshToken: 'test',
        userId: 'liz',
        username: 'liz',
        fullName: 'Liz',
        roleName: 'USUARIO',
        accessTokenExpiresAt: now.add(const Duration(minutes: 15)),
        sessionExpiresAt: now.add(const Duration(days: 1)),
      ),
      rememberMe: false,
    );
  });
  tearDown(() async => AuthSessionManager.instance.clearLocalSession());
  testWidgets(
    'create group selects users by name and returns the created group',
    (tester) async {
      bool created = false;
      final api = ChatApi(
        client: MockClient((request) async {
          if (request.url.path.endsWith('/search')) {
            return http.Response('[{"id":"ana","username":"ana"}]', 200);
          }
          expect(jsonDecode(request.body), {
            'name': 'Lecturas',
            'participantIds': ['ana'],
          });
          created = true;
          return http.Response(jsonEncode(group()), 200);
        }),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => Scaffold(
              body: TextButton(
                onPressed: () async {
                  await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) =>
                          CreateGroupPage(api: api, currentUserId: 'liz'),
                    ),
                  );
                },
                child: const Text('Abrir'),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Abrir'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'Lecturas');
      await tester.tap(find.text('Elegir participantes'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'an');
      await tester.tap(find.byTooltip('Buscar'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(CheckboxListTile));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Listo'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Crear grupo'));
      await tester.pumpAndSettle();
      expect(created, isTrue);
      await tester.pumpWidget(const SizedBox.shrink());
      api.close();
    },
  );
  testWidgets('admin transfer updates membership controls', (tester) async {
    String owner = 'liz';
    final api = ChatApi(
      client: MockClient((request) async {
        if (request.url.path.endsWith('/usernames')) {
          return http.Response('{"liz":"Liz","ana":"Ana"}', 200);
        }
        if (request.url.path.endsWith('/admin')) owner = 'ana';
        return http.Response(jsonEncode(group(owner: owner)), 200);
      }),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: GroupMembersPage(
          api: api,
          conversation: ChatConversation.fromJson(group()),
          currentUserId: 'liz',
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Agregar participante'), findsOneWidget);
    await tester.tap(find.byType(PopupMenuButton<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Transferir administración'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(TextButton, 'Confirmar'));
    await tester.pumpAndSettle();
    expect(owner, 'ana');
    expect(find.text('Agregar participante'), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
    api.close();
  });
}
