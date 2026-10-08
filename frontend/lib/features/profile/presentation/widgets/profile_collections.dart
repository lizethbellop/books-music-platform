import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/exceptions/profile_api_exception.dart';
import '../../data/models/create_user_list_request.dart';
import '../../data/models/preferences_model.dart';
import '../../data/models/user_list_detail_model.dart';
import '../../data/models/user_list_model.dart';
import '../../data/services/profile_api_service.dart';
import '../../data/models/list_element_model.dart';
import '../../data/models/add_list_element_request.dart';
import 'list_content_search_dialog.dart';
import 'collection_poster_strip.dart';
import '../../data/models/add_preference_element_request.dart';

class ProfileCollections extends StatefulWidget {
  final String userId;

  const ProfileCollections({super.key, required this.userId});

  @override
  State<ProfileCollections> createState() => _ProfileCollectionsState();
}

class _ProfileCollectionsState extends State<ProfileCollections> {
  final ProfileApiService _api = ProfileApiService();

  final Map<String, Future<UserListDetailModel>> _listDetails = {};
  late Future<PreferencesModel> _preferencesFuture;
  late Future<List<UserListModel>> _listsFuture;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() {
    _listDetails.clear();
    _preferencesFuture = _api.getOwnPreferences(userId: widget.userId);
    _listsFuture = _api.getOwnLists(userId: widget.userId);
  }

  bool _preferencesBusy = false;

  Future<void> _changePreference(Future<void> Function() action) async {
    if (_preferencesBusy) return;
    setState(() => _preferencesBusy = true);
    try {
      await action();
      if (mounted) {
        setState(() {
          _preferencesFuture = _api.getOwnPreferences(userId: widget.userId);
        });
      }
    } catch (error) {
      if (mounted) _showError(error);
    } finally {
      if (mounted) setState(() => _preferencesBusy = false);
    }
  }

  Future<void> _addPreference() async {
    if (_preferencesBusy) return;
    final selection = await showDialog<ListContentSelection>(
      context: context,
      builder: (_) => const ListContentSearchDialog(),
    );
    if (!mounted || selection == null) return;
    await _changePreference(() async {
      await _api.addPreferenceElement(
        userId: widget.userId,
        request: AddPreferenceElementRequest(
          elementType: selection.elementType,
          referenceId: selection.referenceId,
        ),
      );
    });
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
                    decoration: const InputDecoration(labelText: 'Nombre'),
                    validator: (value) => value == null || value.trim().isEmpty
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
                    description: descriptionController.text.trim().isEmpty
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

      await _api.createList(userId: widget.userId, request: request);

      if (!mounted) return;
      _refresh();
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Lista creada')));
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
        barrierDismissible: false,
        builder: (dialogContext) =>
            _ListDetailDialog(detail: detail, userId: widget.userId),
      );
      if (mounted) setState(() => _listDetails.remove(list.id));
    } catch (error) {
      if (!mounted) return;
      _showError(error);
    }
  }

  void _showError(Object error) {
    final message = error is ProfileApiException
        ? error.message
        : 'No fue posible conectarse con el servicio de Perfil.';

    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Expanded(
                  child: _SectionTitle(
                    title: 'Mis preferencias',
                    icon: Icons.favorite_border,
                  ),
                ),
                IconButton(
                  tooltip: 'Actualizar preferencias',
                  onPressed: _preferencesBusy ? null : _refresh,
                  icon: const Icon(Icons.refresh),
                ),
                IconButton(
                  tooltip: 'Agregar preferencia',
                  onPressed: _preferencesBusy ? null : _addPreference,
                  icon: const Icon(Icons.add),
                ),
              ],
            ),
            if (_preferencesBusy) const LinearProgressIndicator(),
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
                      : CollectionPosterStrip(
                          elements: elements,
                          busy: _preferencesBusy,
                          removeLabel: 'Quitar preferencia',
                          onRemove: (element) => _changePreference(
                            () => _api.removePreferenceElement(
                              userId: widget.userId,
                              elementId: element.id,
                            ),
                          ),
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

                return Column(
                  children: [
                    for (final list in lists)
                      Container(
                        width: double.infinity,
                        margin: const EdgeInsets.only(bottom: 20),
                        padding: const EdgeInsets.all(18),
                        decoration: _cardDecoration(),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    list.name,
                                    style: AppTextStyles.cardTitle,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                                TextButton(
                                  onPressed: () => _openList(list),
                                  child: const Text('Abrir'),
                                ),
                              ],
                            ),
                            if (list.description?.isNotEmpty == true)
                              Text(
                                list.description!,
                                style: AppTextStyles.secondary,
                              ),
                            const SizedBox(height: 12),
                            FutureBuilder<UserListDetailModel>(
                              future: _listDetails.putIfAbsent(
                                list.id,
                                () => _api.getListDetail(
                                  userId: widget.userId,
                                  listId: list.id,
                                ),
                              ),
                              builder: (context, detail) {
                                if (detail.hasError) {
                                  return _ErrorCard(
                                    onRetry: () => setState(
                                      () => _listDetails.remove(list.id),
                                    ),
                                  );
                                }
                                if (!detail.hasData) {
                                  return const _LoadingCard();
                                }
                                if (detail.data!.elements.isEmpty) {
                                  return Text(
                                    'Esta lista todavía está vacía.',
                                    style: AppTextStyles.secondary,
                                  );
                                }
                                return CollectionPosterStrip(
                                  elements: detail.data!.elements,
                                );
                              },
                            ),
                          ],
                        ),
                      ),
                  ],
                );
              },
            ),
          ],
        );
      },
    );
  }
}

class _ListDetailDialog extends StatefulWidget {
  final UserListDetailModel detail;
  final String userId;

  const _ListDetailDialog({required this.detail, required this.userId});

  @override
  State<_ListDetailDialog> createState() => _ListDetailDialogState();
}

class _ListDetailDialogState extends State<_ListDetailDialog> {
  final _api = ProfileApiService();

  late UserListDetailModel _detail = widget.detail;
  bool _busy = false;
  String? _errorMessage;

  Future<void> _reloadDetail() async {
    final updated = await _api.getListDetail(
      userId: widget.userId,
      listId: _detail.id,
    );

    if (!mounted) return;

    setState(() {
      _detail = updated;
    });
  }

  Future<void> _refresh() async {
    if (_busy) return;

    setState(() {
      _busy = true;
      _errorMessage = null;
    });

    try {
      await _reloadDetail();
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _errorMessage = 'No se pudo actualizar la lista.';
      });
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _runChange(Future<void> Function() action) async {
    if (_busy) return;

    setState(() {
      _busy = true;
      _errorMessage = null;
    });

    var saved = false;

    try {
      await action();
      saved = true;
      await _reloadDetail();
    } catch (error) {
      if (!mounted) return;

      setState(() {
        _errorMessage = saved
            ? 'El cambio se guardó, pero no pudimos recargar '
                  'la lista. Pulsa Actualizar.'
            : error is ProfileApiException
            ? error.message
            : 'No se pudo completar el cambio. '
                  'Actualiza la lista para comprobar su estado.';
      });
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _addContent() async {
    if (_busy) return;

    setState(() {
      _busy = true;
      _errorMessage = null;
    });

    ListContentSelection? selection;

    try {
      selection = await showDialog<ListContentSelection>(
        context: context,
        builder: (_) => const ListContentSearchDialog(),
      );
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }

    if (!mounted || selection == null) return;

    final chosen = selection;

    final alreadyExists = _detail.elements.any(
      (element) =>
          element.elementType == chosen.elementType &&
          element.referenceId == chosen.referenceId,
    );

    if (alreadyExists) {
      setState(() {
        _errorMessage = 'Ese contenido ya está en esta lista.';
      });
      return;
    }

    await _runChange(() async {
      await _api.addListElement(
        userId: widget.userId,
        listId: _detail.id,
        request: AddListElementRequest(
          elementType: chosen.elementType,
          referenceId: chosen.referenceId,
        ),
      );
    });
  }

  Future<void> _removeElement(ListElementModel element) async {
    await _runChange(() async {
      await _api.removeListElement(
        userId: widget.userId,
        listId: _detail.id,
        elementId: element.id,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_busy,
      child: AlertDialog(
        backgroundColor: AppColors.warmWhite,
        title: Text(
          _detail.name,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: AppTextStyles.sectionTitle,
        ),
        content: SizedBox(
          width: 520,
          height: MediaQuery.sizeOf(context).height * 0.55,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_busy) ...[
                const LinearProgressIndicator(),
                const SizedBox(height: 12),
              ],
              if (_errorMessage != null) ...[
                Text(_errorMessage!, style: AppTextStyles.secondary),
                const SizedBox(height: 12),
              ],
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (_detail.description?.isNotEmpty == true) ...[
                        Text(_detail.description!, style: AppTextStyles.body),
                        const SizedBox(height: 16),
                      ],
                      if (_detail.elements.isEmpty)
                        Text(
                          'Esta lista todavía está vacía.',
                          style: AppTextStyles.secondary,
                        )
                      else
                        CollectionPosterStrip(
                          elements: _detail.elements,
                          busy: _busy,
                          onRemove: _removeElement,
                        ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: _busy ? null : _refresh,
            child: const Text('Actualizar'),
          ),
          FilledButton.icon(
            onPressed: _busy ? null : _addContent,
            icon: const Icon(Icons.add),
            label: const Text('Agregar contenido'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.lavender,
              foregroundColor: AppColors.ink,
            ),
          ),
          TextButton(
            onPressed: _busy ? null : () => Navigator.pop(context),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionTitle({required this.title, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: AppColors.ink),
        const SizedBox(width: 10),
        Flexible(child: Text(title, style: AppTextStyles.sectionTitle)),
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
        TextButton(onPressed: onRetry, child: const Text('Reintentar')),
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
