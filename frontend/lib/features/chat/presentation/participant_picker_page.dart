import 'package:flutter/material.dart';

import '../data/chat_api.dart';
import '../data/chat_models.dart';

class ParticipantPickerPage extends StatefulWidget {
  final ChatApi api;
  final Set<String> excludedIds;
  final int maxSelection;
  const ParticipantPickerPage({
    super.key,
    required this.api,
    required this.excludedIds,
    required this.maxSelection,
  });
  @override
  State<ParticipantPickerPage> createState() => _ParticipantPickerPageState();
}

class _ParticipantPickerPageState extends State<ParticipantPickerPage> {
  final _query = TextEditingController();
  final Map<String, ChatRecipient> _selected = {};
  List<ChatRecipient> _results = [];
  bool _loading = false;
  String? _error;
  int _generation = 0;
  Future<void> _search() async {
    final query = _query.text.trim();
    if (query.length < 2) {
      setState(() => _error = 'Escribe al menos dos caracteres.');
      return;
    }
    final version = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await widget.api.search(query);
      if (mounted && version == _generation) {
        setState(
          () => _results = result
              .where((u) => !widget.excludedIds.contains(u.id))
              .toList(),
        );
      }
    } catch (e) {
      if (mounted && version == _generation) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted && version == _generation) setState(() => _loading = false);
    }
  }

  @override
  void dispose() {
    _generation++;
    _query.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Elegir participantes'),
      actions: [
        TextButton(
          onPressed: _selected.isEmpty
              ? null
              : () => Navigator.pop(context, _selected.values.toList()),
          child: const Text('Listo'),
        ),
      ],
    ),
    body: Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _query,
                  maxLength: 40,
                  decoration: const InputDecoration(
                    labelText: 'Nombre de usuario',
                  ),
                  onSubmitted: (_) => _search(),
                ),
              ),
              IconButton(
                onPressed: _loading ? null : _search,
                tooltip: 'Buscar',
                icon: const Icon(Icons.search),
              ),
            ],
          ),
        ),
        Text('${_selected.length} de ${widget.maxSelection} seleccionados'),
        ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 160),
          child: SingleChildScrollView(
            child: Wrap(
              children: _selected.values
                  .map(
                    (u) => Padding(
                      padding: const EdgeInsets.all(4),
                      child: InputChip(
                        label: Text(u.username),
                        onDeleted: () => setState(() => _selected.remove(u.id)),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ),
        if (_loading) const LinearProgressIndicator(),
        if (_error != null)
          Padding(padding: const EdgeInsets.all(12), child: Text(_error!)),
        Expanded(
          child: ListView(
            children: _results
                .map(
                  (u) => CheckboxListTile(
                    title: Text(u.username),
                    value: _selected.containsKey(u.id),
                    onChanged: (value) {
                      if (value == true &&
                          _selected.length >= widget.maxSelection) {
                        setState(
                          () =>
                              _error = 'Alcanzaste el límite de participantes.',
                        );
                        return;
                      }
                      setState(() {
                        if (value == true) {
                          _selected[u.id] = u;
                        } else {
                          _selected.remove(u.id);
                        }
                      });
                    },
                  ),
                )
                .toList(),
          ),
        ),
      ],
    ),
  );
}
