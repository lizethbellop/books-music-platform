import 'package:flutter/material.dart';

import '../data/chat_api.dart';
import '../data/chat_models.dart';
import 'participant_picker_page.dart';

class CreateGroupPage extends StatefulWidget {
  final ChatApi api;
  final String currentUserId;
  const CreateGroupPage({
    super.key,
    required this.api,
    required this.currentUserId,
  });
  @override
  State<CreateGroupPage> createState() => _CreateGroupPageState();
}

class _CreateGroupPageState extends State<CreateGroupPage> {
  final _name = TextEditingController();
  List<ChatRecipient> _participants = [];
  bool _saving = false;
  String? _error;
  Future<void> _pick() async {
    final result = await Navigator.push<List<ChatRecipient>>(
      context,
      MaterialPageRoute(
        builder: (_) => ParticipantPickerPage(
          api: widget.api,
          excludedIds: {widget.currentUserId},
          maxSelection: 49,
        ),
      ),
    );
    if (mounted && result != null) setState(() => _participants = result);
  }

  Future<void> _create() async {
    if (_saving) return;
    if (_name.text.trim().isEmpty || _participants.isEmpty) {
      setState(
        () => _error = 'Escribe un nombre y elige al menos otra persona.',
      );
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final group = await widget.api.createGroup(
        _name.text.trim(),
        _participants.map((p) => p.id).toList(),
      );
      if (mounted) Navigator.pop(context, group);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Crear grupo')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          controller: _name,
          maxLength: 100,
          decoration: const InputDecoration(labelText: 'Nombre del grupo'),
        ),
        const Text(
          'Tú administrarás el grupo. Cada persona verá el historial desde que se incorpore.',
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: _saving ? null : _pick,
          icon: const Icon(Icons.group_add_outlined),
          label: const Text('Elegir participantes'),
        ),
        ..._participants.map(
          (u) => ListTile(
            title: Text(u.username),
            trailing: IconButton(
              tooltip: 'Quitar selección',
              icon: const Icon(Icons.close),
              onPressed: _saving
                  ? null
                  : () => setState(
                      () => _participants.removeWhere((p) => p.id == u.id),
                    ),
            ),
          ),
        ),
        if (_error != null)
          Padding(padding: const EdgeInsets.all(8), child: Text(_error!)),
        FilledButton(
          onPressed: _saving ? null : _create,
          child: Text(_saving ? 'Creando…' : 'Crear grupo'),
        ),
      ],
    ),
  );
}
