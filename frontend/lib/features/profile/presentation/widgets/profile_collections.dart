import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/exceptions/profile_api_exception.dart';
import '../../data/models/create_user_list_request.dart';
import '../../data/models/preferences_model.dart';
import '../../data/models/user_list_detail_model.dart';
import '../../data/models/user_list_model.dart';
import '../../data/services/profile_api_service.dart';

class ProfileCollections extends StatefulWidget {
  final String userId;

  const ProfileCollections({
    super.key,
    required this.userId,
  });

  @override
  State<ProfileCollections> createState() => _ProfileCollectionsState();
}

class _ProfileCollectionsState extends State<ProfileCollections> {
  final ProfileApiService _api = ProfileApiService();

  late Future<PreferencesModel> _preferencesFuture;
  late Future<List<UserListModel>> _listsFuture;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() {
    _preferencesFuture = _api.getOwnPreferences(userId: widget.userId);
    _listsFuture = _api.getOwnLists(userId: widget.userId);
  }

  void _refresh() {
    setState(_reload);
  }

  Future<void> _createList() async {
    final nameController = TextEditingController();
    final descriptionController = TextEditingController();
    final formKey = GlobalKey<FormState>();

    try {
      final request = await showDialog<CreateUserListRequest>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          backgroundColor: AppColors.warmWhite,
          title: Text('Crear lista', style: AppTextStyles.sectionTitle),
          content: SizedBox(
            width: 420,
            child: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: nameController,
                    maxLength: 100,
                    decoration: const InputDecoration(
                      labelText: 'Nombre',
                    ),
                    validator: (value) =>
                        value == null || value.trim().isEmpty
                            ? 'Escribe un nombre'
                            : null,
                  ),
                  TextFormField(
                    controller: descriptionController,
                    maxLength: 500,
                    decoration: const InputDecoration(
                      labelText: 'Descripción (opcional)',
                    ),
                    maxLines: 2,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () {
                if (!formKey.currentState!.validate()) return;

                Navigator.pop(
                  dialogContext,
                  CreateUserListRequest(
                    name: nameController.text.trim(),
                    description:
                        descriptionController.text.trim().isEmpty
                            ? null
                            : descriptionController.text.trim(),
                  ),
                );
              },
              child: const Text('Crear'),
            ),
          ],
        ),
      );

      if (request == null || !mounted) return;

      await _api.createList(
        userId: widget.userId,
        request: request,
      );

      if (!mounted) return;
      _refresh();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lista creada')),
      );
    } catch (error) {
      if (!mounted) return;
      _showError(error);
    } finally {
      nameController.dispose();
      descriptionController.dispose();
    }
  }

  Future<void> _openList(UserListModel list) async {
    try {
      final detail = await _api.getListDetail(
        userId: widget.userId,
        listId: list.id,
      );

      if (!mounted) return;

      await showDialog<void>(
        context: context,
        builder: (dialogContext) => _ListDetailDialog(detail: detail),
      );
    } catch (error) {
      if (!mounted) return;
      _showError(error);
    }
  }

  void _showError(Object error) {
    final message = error is ProfileApiException
        ? error.message
        : 'No fue posible conectarse con el servicio de Perfil.';

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop = constraints.maxWidth >= 700;

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _SectionTitle(
              title: 'Mis preferencias',
              icon: Icons.favorite_border,
            ),
            const SizedBox(height: 14),
            FutureBuilder<PreferencesModel>(
              future: _preferencesFuture,
              builder: (context, snapshot) {
                if (!snapshot.hasData && !snapshot.hasError) {
                  return const _LoadingCard();
                }
                if (snapshot.hasError) {
                  return _ErrorCard(onRetry: _refresh);
                }

                final elements = snapshot.data!.elements;
                return Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(18),
                  decoration: _cardDecoration(),
                  child: elements.isEmpty
                      ? Text(
                          'Aún no tienes preferencias guardadas.',
                          style: AppTextStyles.secondary,
                        )
                      : Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            for (final element in elements)
                              Chip(
                                label: Text(
                                  '${_typeLabel(element.elementType)}'
                                  ' · ${element.referenceId}',
                                ),
                                backgroundColor:
                                    element.elementType == 'BOOK' ||
                                            element.elementType == 'AUTHOR'
                                        ? AppColors.lilac
                                        : AppColors.mint,
                                side: BorderSide.none,
                              ),
                          ],
                        ),
                );
              },
            ),
            const SizedBox(height: 32),
            Row(
              children: [
                const Expanded(
                  child: _SectionTitle(
                    title: 'Mis listas',
                    icon: Icons.collections_bookmark_outlined,
                  ),
                ),
                FilledButton.icon(
                  onPressed: _createList,
                  icon: const Icon(Icons.add),
                  label: const Text('Crear lista'),
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.lavender,
                    foregroundColor: AppColors.ink,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            FutureBuilder<List<UserListModel>>(
              future: _listsFuture,
              builder: (context, snapshot) {
                if (!snapshot.hasData && !snapshot.hasError) {
                  return const _LoadingCard();
                }
                if (snapshot.hasError) {
                  return _ErrorCard(onRetry: _refresh);
                }

                final lists = snapshot.data!;
                if (lists.isEmpty) {
                  return Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
                    decoration: _cardDecoration(),
                    child: Text(
                      'Todavía no has creado ninguna lista.',
                      style: AppTextStyles.secondary,
                    ),
                  );
                }

                return GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: lists.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: isDesktop ? 2 : 1,
                    crossAxisSpacing: 14,
                    mainAxisSpacing: 14,
                    mainAxisExtent: 130,
                  ),
                  itemBuilder: (context, index) {
                    final list = lists[index];
                    return InkWell(
                      onTap: () => _openList(list),
                      borderRadius: BorderRadius.circular(16),
                      child: Container(
                        padding: const EdgeInsets.all(18),
                        decoration: _cardDecoration(),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Icon(
                              Icons.library_music_outlined,
                              color: AppColors.ink,
                            ),
                            const Spacer(),
                            Text(
                              list.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AppTextStyles.cardTitle,
                            ),
                            Text(
                              list.description?.isNotEmpty == true
                                  ? list.description!
                                  : 'Toca para ver esta lista',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AppTextStyles.secondary,
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ],
        );
      },
    );
  }
}

class _ListDetailDialog extends StatelessWidget {
  final UserListDetailModel detail;

  const _ListDetailDialog({required this.detail});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.warmWhite,
      title: Text(detail.name, style: AppTextStyles.sectionTitle),
      content: SizedBox(
        width: 480,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (detail.description?.isNotEmpty == true) ...[
              Text(detail.description!, style: AppTextStyles.body),
              const SizedBox(height: 16),
            ],
            if (detail.elements.isEmpty)
              Text(
                'Esta lista todavía está vacía.',
                style: AppTextStyles.secondary,
              )
            else
              for (final element in detail.elements)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                    element.elementType == 'BOOK'
                        ? Icons.book_outlined
                        : Icons.music_note_outlined,
                    color: AppColors.ink,
                  ),
                  title: Text(
                    '${_typeLabel(element.elementType)}'
                    ' · ${element.referenceId}',
                  ),
                ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cerrar'),
        ),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionTitle({
    required this.title,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: AppColors.ink),
        const SizedBox(width: 10),
        Flexible(
          child: Text(title, style: AppTextStyles.sectionTitle),
        ),
      ],
    );
  }
}

class _LoadingCard extends StatelessWidget {
  const _LoadingCard();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.all(24),
      child: Center(child: CircularProgressIndicator()),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  final VoidCallback onRetry;

  const _ErrorCard({required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Text('No se pudo cargar esta sección.')),
        TextButton(
          onPressed: onRetry,
          child: const Text('Reintentar'),
        ),
      ],
    );
  }
}

BoxDecoration _cardDecoration() {
  return BoxDecoration(
    color: AppColors.warmWhite,
    borderRadius: BorderRadius.circular(16),
    border: Border.all(color: AppColors.border),
  );
}

String _typeLabel(String type) {
  return switch (type) {
    'BOOK' => 'Libro',
    'SONG' => 'Canción',
    'ARTIST' => 'Artista',
    'AUTHOR' => 'Autor',
    _ => type,
  };
}