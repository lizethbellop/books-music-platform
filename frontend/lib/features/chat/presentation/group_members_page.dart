import 'dart:async';

import 'package:flutter/material.dart';

import '../data/chat_api.dart';
import '../data/chat_models.dart';
import 'participant_picker_page.dart';

class GroupMembersPage extends StatefulWidget {
  final ChatApi api;
  final ChatConversation conversation;
  final String currentUserId;
  const GroupMembersPage({
    super.key,
    required this.api,
    required this.conversation,
    required this.currentUserId,
  });
  @override
  State<GroupMembersPage> createState() => _GroupMembersPageState();
}

class _GroupMembersPageState extends State<GroupMembersPage> {
  late ChatConversation _group = widget.conversation;
  Map<String, String> _names = {};
  bool _busy = false;
  String? _error;
  bool get _admin => _group.ownerId == widget.currentUserId;
  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load() => _change(() => widget.api.details(_group.id));
  Future<void> _change(Future<ChatConversation> Function() operation) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final group = await operation();
      Map<String, String> names = {};
      try {
        names = await widget.api.usernames(group.participantIds);
      } catch (_) {
        /* Nombres opcionales. */
      }
      if (mounted) {
        setState(() {
          _group = group;
          _names = names;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<bool> _confirm(String text) async =>
      await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Confirmar'),
          content: Text(text),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancelar'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Confirmar'),
            ),
          ],
        ),
      ) ??
      false;
  Future<void> _rename() async {
    String draftName = _group.name ?? '';
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Nombre del grupo'),
        content: TextFormField(
          initialValue: draftName,
          maxLength: 100,
          onChanged: (value) => draftName = value,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, draftName.trim()),
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (name != null && mounted) {
      await _change(() => widget.api.renameGroup(_group.id, name));
    }
  }

  Future<void> _add() async {
    final selected = await Navigator.push<List<ChatRecipient>>(
      context,
      MaterialPageRoute(
        builder: (_) => ParticipantPickerPage(
          api: widget.api,
          excludedIds: _group.participantIds.toSet(),
          maxSelection: 1,
        ),
      ),
    );
    if (selected != null && selected.isNotEmpty && mounted) {
      await _change(
        () => widget.api.addParticipant(_group.id, selected.first.id),
      );
    }
  }

  Future<void> _remove(String id) async {
    if (await _confirm(
      '¿Retirar a esta persona del grupo? Sus mensajes anteriores se conservarán, pero perderá acceso.',
    )) {
      if (mounted) {
        await _change(() => widget.api.removeParticipant(_group.id, id));
      }
    }
  }

  Future<void> _transfer(String id) async {
    if (await _confirm(
      '¿Transferir la administración a esta persona? Dejarás de administrar el grupo.',
    )) {
      if (mounted) await _change(() => widget.api.transferAdmin(_group.id, id));
    }
  }

  Future<void> _leave() async {
    if (!await _confirm(
      '¿Salir del grupo? Si lo administras, se asignará un relevo disponible. Si queda vacío, se cerrará.',
    )) {
      return;
    }
    if (!mounted) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.leaveGroup(_group.id);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Participantes')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ListTile(
          title: Text(_group.name ?? 'Grupo'),
          subtitle: Text('${_group.participantIds.length} participantes'),
          trailing: _admin
              ? IconButton(
                  onPressed: _busy ? null : _rename,
                  tooltip: 'Cambiar nombre',
                  icon: const Icon(Icons.edit_outlined),
                )
              : null,
        ),
        if (_busy) const LinearProgressIndicator(),
        if (_error != null)
          Text(
            _error!,
            style: TextStyle(color: Theme.of(context).colorScheme.error),
          ),
        if (_admin)
          OutlinedButton.icon(
            onPressed: _busy || _group.participantIds.length >= 50
                ? null
                : _add,
            icon: const Icon(Icons.person_add_outlined),
            label: const Text('Agregar participante'),
          ),
        ..._group.participantIds.map(
          (id) => ListTile(
            title: Text(_names[id] ?? 'Participante'),
            subtitle: Text(
              id == _group.ownerId
                  ? 'Administración'
                  : (id == widget.currentUserId ? 'Tú' : 'Participante'),
            ),
            trailing: _admin && id != widget.currentUserId
                ? PopupMenuButton<String>(
                    enabled: !_busy,
                    onSelected: (action) {
                      if (action == 'remove') {
                        unawaited(_remove(id));
                      } else {
                        unawaited(_transfer(id));
                      }
                    },
                    itemBuilder: (_) => const [
                      PopupMenuItem(
                        value: 'admin',
                        child: Text('Transferir administración'),
                      ),
                      PopupMenuItem(
                        value: 'remove',
                        child: Text('Retirar del grupo'),
                      ),
                    ],
                  )
                : null,
          ),
        ),
        const SizedBox(height: 24),
        OutlinedButton(
          onPressed: _busy ? null : _leave,
          child: const Text('Salir del grupo'),
        ),
      ],
    ),
  );
}
