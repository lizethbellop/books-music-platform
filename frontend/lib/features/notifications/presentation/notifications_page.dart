import 'dart:async';

import 'package:flutter/material.dart';

import '../../auth/data/services/auth_session_manager.dart';
import '../../chat/data/chat_api.dart';
import '../../chat/data/chat_connection.dart';
import '../../chat/data/chat_models.dart';
import '../../chat/presentation/conversation_page.dart';
import '../data/notification_api.dart';

class NotificationsPage extends StatefulWidget {
  final NotificationApi? api;
  const NotificationsPage({super.key, this.api});
  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  late final NotificationApi _api = widget.api ?? NotificationApi();
  late final String? _userId = AuthSessionManager.instance.session?.userId;
  final List<AppNotification> _items = [];
  Timer? _refresh;
  int? _nextPage;
  int _unread = 0, _generation = 0;
  bool _loading = false, _actionBusy = false, _unreadOnly = false;
  String? _error;
  bool get _authorized =>
      _userId != null && AuthSessionManager.instance.session?.userId == _userId;
  @override
  void initState() {
    super.initState();
    AuthSessionManager.instance.addListener(_sessionChanged);
    if (_authorized) {
      unawaited(_load());
      _refresh = Timer.periodic(const Duration(seconds: 15), (_) {
        if (!_loading && !_actionBusy && _items.length <= 20) {
          unawaited(_load());
        }
      });
    }
  }

  void _sessionChanged() {
    if (_authorized || !mounted) return;
    _generation++;
    _refresh?.cancel();
    setState(() {
      _items.clear();
      _unread = 0;
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) Navigator.of(context).popUntil((route) => route.isFirst);
    });
  }

  Future<void> _load({bool more = false}) async {
    if (!_authorized || (more && _loading)) return;
    final version = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await _api.list(
        page: more ? (_nextPage ?? 0) : 0,
        unreadOnly: _unreadOnly,
      );
      final count = await _api.unreadCount();
      if (!mounted || !_authorized || version != _generation) return;
      setState(() {
        if (!more) _items.clear();
        for (final item in page.items) {
          if (!_items.any((n) => n.id == item.id)) _items.add(item);
        }
        _nextPage = page.nextPage;
        _unread = count;
      });
    } catch (e) {
      if (mounted && version == _generation) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted && version == _generation) setState(() => _loading = false);
    }
  }

  Future<void> _markRead(AppNotification item, {bool open = false}) async {
    if (_actionBusy) return;
    setState(() {
      _actionBusy = true;
      _error = null;
    });
    try {
      await _api.markRead(item.id);
      if (!mounted || !_authorized) return;
      setState(() {
        if (!item.isRead && _unread > 0) _unread--;
        final index = _items.indexWhere((n) => n.id == item.id);
        if (index >= 0) {
          if (_unreadOnly) {
            _items.removeAt(index);
          } else {
            _items[index] = item.asRead();
          }
        }
      });
      if (open &&
          item.targetType == 'CONVERSATION' &&
          item.type != 'GROUP_MEMBER_REMOVED') {
        final chat = ChatApi();
        final connection = ChatConnection(chat);
        unawaited(connection.connect());
        try {
          await Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (_) => ConversationPage(
                conversation: ChatConversation(
                  id: item.targetId,
                  participantIds: [_userId!, item.actorId],
                ),
                title: 'Conversación',
                currentUserId: _userId,
                api: chat,
                connection: connection,
              ),
            ),
          );
        } finally {
          connection.close();
          chat.close();
        }
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _actionBusy = false);
    }
  }

  Future<void> _markAll() async {
    setState(() {
      _actionBusy = true;
      _error = null;
    });
    try {
      await _api.markAllRead();
      if (mounted && _authorized) await _load();
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _actionBusy = false);
    }
  }

  @override
  void dispose() {
    _generation++;
    _refresh?.cancel();
    AuthSessionManager.instance.removeListener(_sessionChanged);
    if (widget.api == null) _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_authorized) {
      return Scaffold(
        appBar: AppBar(title: const Text('Notificaciones')),
        body: const Center(
          child: Text('Inicia sesión para ver tus notificaciones.'),
        ),
      );
    }
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notificaciones'),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            onPressed: _loading ? null : () => _load(),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Marcar todas como leídas',
            onPressed: _actionBusy || _unread == 0 ? null : _markAll,
            icon: const Icon(Icons.done_all),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Expanded(child: Text('$_unread sin leer')),
                FilterChip(
                  label: const Text('Solo sin leer'),
                  selected: _unreadOnly,
                  onSelected: (value) {
                    setState(() => _unreadOnly = value);
                    unawaited(_load());
                  },
                ),
              ],
            ),
          ),
          if (_loading || _actionBusy) const LinearProgressIndicator(),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ),
          Expanded(
            child: _items.isEmpty && !_loading
                ? const Center(
                    child: Text('No tienes notificaciones para mostrar.'),
                  )
                : ListView(
                    children: [
                      ..._items.map(
                        (item) => ListTile(
                          tileColor: item.isRead
                              ? null
                              : Theme.of(context).colorScheme.primaryContainer
                                    .withValues(alpha: 0.3),
                          leading: Icon(
                            item.targetType == 'CONVERSATION'
                                ? Icons.chat_bubble_outline
                                : Icons.notifications_outlined,
                          ),
                          title: Text(
                            item.title,
                            style: TextStyle(
                              fontWeight: item.isRead
                                  ? FontWeight.normal
                                  : FontWeight.bold,
                            ),
                          ),
                          subtitle: Text(item.body),
                          onTap: _actionBusy
                              ? null
                              : () => _markRead(item, open: true),
                          trailing: item.isRead
                              ? const Icon(Icons.done, semanticLabel: 'Leída')
                              : IconButton(
                                  tooltip: 'Marcar como leída',
                                  onPressed: _actionBusy
                                      ? null
                                      : () => _markRead(item),
                                  icon: const Icon(
                                    Icons.mark_email_read_outlined,
                                  ),
                                ),
                        ),
                      ),
                      if (_nextPage != null)
                        TextButton(
                          onPressed: _loading ? null : () => _load(more: true),
                          child: const Text('Ver más'),
                        ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}
