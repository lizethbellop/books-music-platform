import 'dart:async';

import 'package:flutter/material.dart';

import '../../auth/data/services/auth_session_manager.dart';
import '../data/chat_api.dart';
import '../data/chat_models.dart';
import '../data/chat_connection.dart';
import 'conversation_page.dart';
import 'create_group_page.dart';

class MessagesPage extends StatefulWidget {
  final ChatApi? api;
  const MessagesPage({super.key, this.api});
  @override
  State<MessagesPage> createState() => _MessagesPageState();
}

class _MessagesPageState extends State<MessagesPage> {
  late final ChatApi _api = widget.api ?? ChatApi();
  late final ChatConnection _connection;
  StreamSubscription<Map<String, dynamic>>? _subscription;
  final _search = TextEditingController();
  final List<ChatConversation> _conversations = [];
  final Map<String, String> _names = {};
  List<ChatRecipient> _recipients = [];
  String? _error;
  bool _loading = false, _searching = false, _opening = false, _live = false;
  int? _nextPage;
  int _searchVersion = 0;
  String get _userId => AuthSessionManager.instance.session?.userId ?? '';
  @override
  void initState() {
    super.initState();
    _connection = ChatConnection(_api);
    _subscription = _connection.events.listen((event) {
      if (!mounted) return;
      setState(() => _live = event['type'] != 'disconnected');
      if (event['type'] == 'ready' ||
          event['type'] == 'message.created' ||
          event['type'] == 'conversation.updated') {
        unawaited(_load());
      }
    });
    unawaited(_load());
    unawaited(_connection.connect());
  }

  Future<void> _load({bool more = false}) async {
    if (_loading) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await _api.conversations(page: more ? (_nextPage ?? 0) : 0);
      Map<String, String> names = {};
      try {
        names = await _api.usernames(
          page.items.expand((c) => c.participantIds),
        );
      } catch (_) {
        /* El chat sigue accesible sin nombres. */
      }
      if (!mounted) return;
      setState(() {
        if (!more) _conversations.clear();
        for (final c in page.items) {
          if (!_conversations.any((old) => old.id == c.id)) {
            _conversations.add(c);
          }
        }
        _nextPage = page.nextPage;
        _names.addAll(names);
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _find() async {
    final query = _search.text.trim();
    final version = ++_searchVersion;
    if (query.length < 2) {
      setState(() => _error = 'Escribe al menos dos caracteres.');
      return;
    }
    setState(() {
      _searching = true;
      _error = null;
    });
    try {
      final result = await _api.search(query);
      if (mounted && version == _searchVersion) {
        setState(
          () => _recipients = result.where((u) => u.id != _userId).toList(),
        );
      }
    } catch (e) {
      if (mounted && version == _searchVersion) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted && version == _searchVersion) {
        setState(() => _searching = false);
      }
    }
  }

  String _title(ChatConversation c) {
    if (c.isGroup) return c.name ?? 'Grupo';
    final other = c.participantIds.where((id) => id != _userId);
    return other.isEmpty
        ? 'Conversación'
        : (_names[other.first] ?? 'Usuario no disponible');
  }

  Future<void> _open(ChatConversation c, String title) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => ConversationPage(
          conversation: c,
          title: title,
          api: _api,
          connection: _connection,
          currentUserId: _userId,
        ),
      ),
    );
    if (mounted) unawaited(_load());
  }

  Future<void> _createGroup() async {
    final group = await Navigator.push<ChatConversation>(
      context,
      MaterialPageRoute(
        builder: (_) => CreateGroupPage(api: _api, currentUserId: _userId),
      ),
    );
    if (group != null && mounted) await _open(group, group.name ?? 'Grupo');
  }

  Future<void> _create(ChatRecipient recipient) async {
    if (_opening) return;
    setState(() {
      _opening = true;
      _error = null;
    });
    try {
      final c = await _api.create(recipient.id);
      if (!mounted) return;
      _names[recipient.id] = recipient.username;
      await _open(c, recipient.username);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _opening = false);
    }
  }

  @override
  void dispose() {
    _searchVersion++;
    _search.dispose();
    unawaited(_subscription?.cancel());
    _connection.close();
    if (widget.api == null) _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Expanded(
                child: Text(
                  'Mensajes',
                  style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold),
                ),
              ),
              Tooltip(
                message: _live
                    ? 'En tiempo real'
                    : 'Reconectando; puedes actualizar manualmente',
                child: Icon(_live ? Icons.wifi : Icons.wifi_off, size: 20),
              ),
              IconButton(
                onPressed: _opening ? null : _createGroup,
                tooltip: 'Crear grupo',
                icon: const Icon(Icons.group_add_outlined),
              ),
              IconButton(
                onPressed: _loading ? null : () => _load(),
                tooltip: 'Actualizar',
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _search,
                  maxLength: 40,
                  decoration: const InputDecoration(
                    labelText: 'Buscar persona por nombre de usuario',
                    counterText: '',
                  ),
                  onSubmitted: (_) => _find(),
                ),
              ),
              IconButton(
                onPressed: _searching ? null : _find,
                tooltip: 'Buscar',
                icon: const Icon(Icons.search),
              ),
            ],
          ),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.all(12),
            child: Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ),
        if (_searching || _loading || _opening) const LinearProgressIndicator(),
        if (_recipients.isNotEmpty)
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 200),
            child: ListView(
              shrinkWrap: true,
              children: _recipients
                  .map(
                    (r) => ListTile(
                      leading: const Icon(Icons.person_outline),
                      title: Text(r.username),
                      trailing: const Icon(Icons.add_comment_outlined),
                      onTap: _opening ? null : () => _create(r),
                    ),
                  )
                  .toList(),
            ),
          ),
        if (!_searching &&
            _search.text.trim().length >= 2 &&
            _recipients.isEmpty)
          const Padding(
            padding: EdgeInsets.all(8),
            child: Text('Busca un nombre para iniciar una conversación.'),
          ),
        const Divider(),
        Expanded(
          child: _conversations.isEmpty && !_loading
              ? const Center(
                  child: Text(
                    'Aún no tienes conversaciones. Busca a alguien para comenzar.',
                  ),
                )
              : ListView(
                  children: [
                    ..._conversations.map(
                      (c) => ListTile(
                        leading: const CircleAvatar(
                          child: Icon(Icons.chat_bubble_outline),
                        ),
                        title: Text(_title(c)),
                        subtitle: Text(
                          c.isGroup
                              ? 'Conversación grupal'
                              : 'Conversación privada',
                        ),
                        onTap: () => _open(c, _title(c)),
                      ),
                    ),
                    if (_nextPage != null)
                      TextButton(
                        onPressed: _loading ? null : () => _load(more: true),
                        child: const Text('Ver más conversaciones'),
                      ),
                  ],
                ),
        ),
      ],
    ),
  );
}
