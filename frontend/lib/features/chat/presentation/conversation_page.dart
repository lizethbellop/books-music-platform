import 'dart:async';

import '../../auth/data/services/auth_session_manager.dart';

import 'package:flutter/material.dart';

import '../data/chat_api.dart';
import '../data/chat_models.dart';
import '../data/chat_connection.dart';
import 'group_members_page.dart';

class ConversationPage extends StatefulWidget {
  final ChatConversation conversation;
  final String title, currentUserId;
  final ChatApi api;
  final ChatConnection connection;
  const ConversationPage({
    super.key,
    required this.conversation,
    required this.title,
    required this.currentUserId,
    required this.api,
    required this.connection,
  });
  @override
  State<ConversationPage> createState() => _ConversationPageState();
}

class _ConversationPageState extends State<ConversationPage> {
  final _draft = TextEditingController();
  final Map<String, ChatMessage> _messages = {};
  StreamSubscription<Map<String, dynamic>>? _subscription;
  ChatHistory? _cursor;
  late ChatConversation _conversation = widget.conversation;
  final Map<String, String> _senderNames = {};
  bool _available = true;
  int _metadataVersion = 0;
  bool _authorized = true;
  String? _error;
  bool _loading = false, _sending = false, _live = false;
  @override
  void initState() {
    super.initState();
    _live = widget.connection.isConnected;
    AuthSessionManager.instance.addListener(_sessionChanged);
    unawaited(_refreshConversation(reloadHistory: true));
    _subscription = widget.connection.events.listen((event) {
      if (!mounted) return;
      if (event['type'] == 'disconnected') {
        setState(() => _live = false);
        return;
      }
      if (event['type'] == 'ready') {
        setState(() => _live = true);
        unawaited(_refreshConversation(reloadHistory: true));
      }
      if (event['type'] == 'conversation.updated' &&
          event['conversationId'] == widget.conversation.id) {
        unawaited(_refreshConversation());
      }
      if (_available && event['type'] == 'message.created') {
        final message = ChatMessage.fromJson(
          event['message'] as Map<String, dynamic>,
        );
        if (message.conversationId == widget.conversation.id) {
          setState(() => _messages[message.id] = message);
        }
      }
    });
  }

  void _sessionChanged() {
    if (AuthSessionManager.instance.session?.userId == widget.currentUserId) {
      return;
    }
    if (!mounted) return;
    setState(() {
      _authorized = false;
      _messages.clear();
      _draft.clear();
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) Navigator.of(context).popUntil((route) => route.isFirst);
    });
  }

  Future<void> _refreshConversation({bool reloadHistory = false}) async {
    final version = ++_metadataVersion;
    final needsHistory = reloadHistory || !_available;
    try {
      final conversation = await widget.api.details(widget.conversation.id);
      if (!mounted || version != _metadataVersion || !_authorized) return;
      setState(() {
        _conversation = conversation;
        _available = true;
        _error = null;
      });
      if (needsHistory) await _load();
    } catch (e) {
      if (!mounted || version != _metadataVersion) return;
      if (e is ChatApiException && e.status == 404) {
        setState(() {
          _available = false;
          _messages.clear();
          _draft.clear();
        });
      } else {
        setState(() => _error = e.toString());
      }
    }
  }

  Future<void> _members() async {
    final left = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => GroupMembersPage(
          api: widget.api,
          conversation: _conversation,
          currentUserId: widget.currentUserId,
        ),
      ),
    );
    if (!mounted) return;
    if (left == true) {
      Navigator.pop(context);
    } else {
      await _refreshConversation();
    }
  }

  Future<void> _load({bool older = false}) async {
    if (_loading || !_available || !_authorized) return;
    setState(() {
      _loading = true;
      _error = null;
      if (!older) _messages.clear();
    });
    try {
      final history = await widget.api.history(
        widget.conversation.id,
        beforeCreatedAt: older ? _cursor?.nextCreatedAt : null,
        beforeId: older ? _cursor?.nextId : null,
      );
      Map<String, String> names = {};
      if (_conversation.isGroup) {
        try {
          names = await widget.api.usernames(
            history.items.map((m) => m.senderId),
          );
        } catch (_) {
          /* El historial no depende de mostrar nombres. */
        }
      }
      if (!mounted || !_available || !_authorized) return;
      setState(() {
        _senderNames.addAll(names);
        for (final m in history.items) {
          _messages[m.id] = m;
        }
        _cursor = history;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _send() async {
    final content = _draft.text;
    if (_sending || content.trim().isEmpty || !_available || !_authorized) {
      return;
    }
    setState(() {
      _sending = true;
      _error = null;
    });
    try {
      final message = await widget.api.send(widget.conversation.id, content);
      if (!mounted) return;
      setState(() {
        _messages[message.id] = message;
        if (_draft.text == content) _draft.clear();
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  void dispose() {
    AuthSessionManager.instance.removeListener(_sessionChanged);
    _metadataVersion++;
    _draft.dispose();
    unawaited(_subscription?.cancel());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_authorized) {
      return const Scaffold(
        body: Center(child: Text('Inicia sesión para continuar.')),
      );
    }
    if (!_available) {
      return Scaffold(
        appBar: AppBar(title: const Text('Conversación no disponible')),
        body: const Center(
          child: Text('Ya no tienes acceso a esta conversación.'),
        ),
      );
    }
    final messages = _messages.values.toList()
      ..sort((a, b) {
        final time = b.createdAt.compareTo(a.createdAt);
        return time != 0 ? time : b.id.compareTo(a.id);
      });
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _conversation.isGroup
              ? (_conversation.name ?? 'Grupo')
              : widget.title,
        ),
        actions: [
          if (_conversation.isGroup)
            IconButton(
              tooltip: 'Participantes del grupo',
              onPressed: _members,
              icon: const Icon(Icons.group_outlined),
            ),
          IconButton(
            tooltip: 'Actualizar historial',
            onPressed: _loading
                ? null
                : () => _refreshConversation(reloadHistory: true),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (!_live)
              const Padding(
                padding: EdgeInsets.all(8),
                child: Text(
                  'Puedes enviar mensajes. El historial se sincroniza al reconectar.',
                ),
              ),
            if (_loading) const LinearProgressIndicator(),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.all(12),
                child: Text(
                  _error!,
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ),
            Expanded(
              child: messages.isEmpty && !_loading
                  ? const Center(child: Text('Envía el primer mensaje.'))
                  : ListView.builder(
                      reverse: true,
                      itemCount:
                          messages.length +
                          ((_cursor?.hasMore ?? false) ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (index == messages.length) {
                          return TextButton(
                            onPressed: _loading
                                ? null
                                : () => _load(older: true),
                            child: const Text('Cargar mensajes anteriores'),
                          );
                        }
                        final m = messages[index];
                        final mine = m.senderId == widget.currentUserId;
                        return Align(
                          alignment: mine
                              ? Alignment.centerRight
                              : Alignment.centerLeft,
                          child: Container(
                            constraints: const BoxConstraints(maxWidth: 480),
                            margin: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 4,
                            ),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(16),
                              color: mine
                                  ? Theme.of(context)
                                        .colorScheme
                                        .primaryContainer
                                  : Theme.of(context)
                                        .colorScheme
                                        .surfaceContainerHighest,
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (_conversation.isGroup)
                                  Text(
                                    mine
                                        ? 'Tú'
                                        : (_senderNames[m.senderId] ??
                                              'Participante'),
                                    style: Theme.of(context)
                                        .textTheme
                                        .labelMedium,
                                  ),
                                SelectableText(m.content),
                                const SizedBox(height: 4),
                                Text(
                                  MaterialLocalizations.of(context)
                                      .formatTimeOfDay(
                                        TimeOfDay.fromDateTime(
                                          m.createdAt.toLocal(),
                                        ),
                                      ),
                                  style: Theme.of(context).textTheme.labelSmall,
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: TextField(
                      controller: _draft,
                      minLines: 1,
                      maxLines: 5,
                      maxLength: 4000,
                      decoration: const InputDecoration(
                        hintText: 'Escribe un mensaje',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: _sending ? null : _send,
                    tooltip: 'Enviar mensaje',
                    icon: _sending
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
