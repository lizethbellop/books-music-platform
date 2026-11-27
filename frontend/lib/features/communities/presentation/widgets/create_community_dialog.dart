import 'package:flutter/material.dart';
import '../../data/models/community_model.dart';

class CreateCommunityDialog extends StatefulWidget {
  const CreateCommunityDialog({super.key});

  @override
  State<CreateCommunityDialog> createState() => _CreateCommunityDialogState();
}

class _CreateCommunityDialogState extends State<CreateCommunityDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();

  String _selectedType = 'MIXTO';
  String _selectedVisibility = 'PUBLICA';

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;

    final description = _descriptionController.text.trim();
    final request = CommunityCreateRequestModel(
      name: _nameController.text.trim(),
      description: description.isEmpty ? null : description,
      type: _selectedType,
      visibility: _selectedVisibility,
    );

    Navigator.of(context).pop(request);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return AlertDialog(
      title: const Text('Crear comunidad'),
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
                  controller: _nameController,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: const InputDecoration(
                    labelText: 'Nombre de la comunidad *',
                    hintText: 'Ej. Club de Jazz y Literatura',
                    border: OutlineInputBorder(),
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'El nombre es obligatorio';
                    }
                    if (value.trim().length < 3) {
                      return 'Escribe al menos 3 caracteres';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                TextFormField(
                  controller: _descriptionController,
                  maxLines: 3,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: const InputDecoration(
                    labelText: 'Descripción (opcional)',
                    hintText: '¿De qué trata este espacio?',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),

                Text(
                  'Temática',
                  style: theme.textTheme.labelLarge,
                ),
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

                Text(
                  'Visibilidad',
                  style: theme.textTheme.labelLarge,
                ),
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
          child: const Text('Crear'),
        ),
      ],
    );
  }
}