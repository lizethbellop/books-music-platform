import 'package:flutter/material.dart';
import '../../data/models/community_model.dart';

class EditCommunityDialog extends StatefulWidget {
  final CommunityModel community;

  const EditCommunityDialog({super.key, required this.community});

  @override
  State<EditCommunityDialog> createState() => _EditCommunityDialogState();
}

class _EditCommunityDialogState extends State<EditCommunityDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _descriptionController =
      TextEditingController(text: widget.community.description ?? '');

  late String _selectedType = widget.community.type.toUpperCase();
  late String _selectedVisibility = widget.community.visibility.toUpperCase();

  @override
  void dispose() {
    _descriptionController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;

    final request = CommunityUpdateRequestModel(
      description: _descriptionController.text.trim(),
      type: _selectedType,
      visibility: _selectedVisibility,
    );

    Navigator.of(context).pop(request);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return AlertDialog(
      title: Text('Editar ${widget.community.name}'),
      content: SizedBox(
        width: 420,
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextFormField(
                  controller: _descriptionController,
                  maxLines: 3,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: const InputDecoration(
                    labelText: 'Descripción',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                Text('Temática', style: theme.textTheme.labelLarge),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: SegmentedButton<String>(
                    segments: const [
                      ButtonSegment(
                        value: 'LIBROS',
                        label: Text('Libros'),
                        icon: Icon(Icons.menu_book_rounded, size: 16),
                      ),
                      ButtonSegment(
                        value: 'MUSICA',
                        label: Text('Música'),
                        icon: Icon(Icons.music_note_rounded, size: 16),
                      ),
                      ButtonSegment(
                        value: 'MIXTO',
                        label: Text('Mixto'),
                        icon: Icon(Icons.auto_awesome_rounded, size: 16),
                      ),
                    ],
                    selected: {_selectedType},
                    onSelectionChanged: (selection) {
                      setState(() => _selectedType = selection.first);
                    },
                  ),
                ),
                const SizedBox(height: 16),
                Text('Visibilidad', style: theme.textTheme.labelLarge),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: SegmentedButton<String>(
                    segments: const [
                      ButtonSegment(
                        value: 'PUBLICA',
                        label: Text('Pública'),
                        icon: Icon(Icons.public_rounded, size: 16),
                      ),
                      ButtonSegment(
                        value: 'PRIVADA',
                        label: Text('Privada'),
                        icon: Icon(Icons.lock_outline_rounded, size: 16),
                      ),
                    ],
                    selected: {_selectedVisibility},
                    onSelectionChanged: (selection) {
                      setState(() => _selectedVisibility = selection.first);
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: _submit,
          child: const Text('Guardar'),
        ),
      ],
    );
  }
}