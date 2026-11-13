import 'dart:async';

import 'package:flutter/material.dart';

import '../../../core/routes/app_routes.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_text_styles.dart';
import '../../books/data/services/books_api_service.dart';
import '../../music/data/services/music_api_service.dart';
import '../../profile/presentation/widgets/list_content_search_dialog.dart'
    show ListContentSelection;
import '../data/preferences_onboarding_api.dart';

class PreferencesOnboardingScreen extends StatefulWidget {
  final String userId;
  final BooksApiService? booksApi;
  final MusicApiService? musicApi;
  final PreferencesOnboardingApi? preferencesApi;
  const PreferencesOnboardingScreen({
    super.key,
    required this.userId,
    this.booksApi,
    this.musicApi,
    this.preferencesApi,
  });

  @override
  State<PreferencesOnboardingScreen> createState() =>
      _PreferencesOnboardingScreenState();
}

class _PreferencesOnboardingScreenState
    extends State<PreferencesOnboardingScreen> {
  late final _books = widget.booksApi ?? BooksApiService();
  late final _music = widget.musicApi ?? MusicApiService();
  late final _preferences = widget.preferencesApi ?? PreferencesOnboardingApi();
  final _query = TextEditingController();
  final _selected = <String, ListContentSelection>{};
  List<ListContentSelection> _results = [];
  Timer? _debounce;
  String _type = 'BOOK';
  bool _searching = false, _searched = false, _saving = false, _ready = false;
  String? _searchError, _saveError;
  int _version = 0;

  @override
  void initState() {
    super.initState();
    _prepare();
  }

  Future<void> _prepare() async {
    setState(() {
      _ready = false;
      _saveError = null;
    });
    try {
      await _preferences.ensureProfile();
      if (mounted) setState(() => _ready = true);
    } catch (_) {
      if (mounted) {
        setState(
          () => _saveError =
              'No pudimos preparar tu perfil. Reintenta antes de guardar.',
        );
      }
    }
  }

  String _key(ListContentSelection item) =>
      '${item.elementType}:${item.referenceId}';

  void _queryChanged(String _) {
    _debounce?.cancel();
    _version++; // invalidate any previous request immediately, including when clearing text.
    setState(() {
      _results = [];
      _searchError = null;
      _searching = false;
      _searched = false;
    });
    if (_query.text.trim().isNotEmpty) {
      _debounce = Timer(const Duration(milliseconds: 400), _search);
    }
  }

  Future<void> _search() async {
    _debounce?.cancel();
    final query = _query.text.trim();
    if (query.isEmpty) return;
    final version = ++_version, type = _type;
    setState(() {
      _searching = true;
      _searched = true;
      _searchError = null;
    });
    try {
      List<ListContentSelection> results;
      if (type == 'BOOK') {
        final books = await _books
            .searchBooks(query)
            .timeout(const Duration(seconds: 15));
        results = books
            .map(
              (b) => ListContentSelection(
                elementType: 'BOOK',
                referenceId: b.externalId,
                title: b.title,
                subtitle: b.authors.join(', '),
                imageUrl: b.coverUrl,
              ),
            )
            .toList();
      } else {
        final music = await _music
            .searchMusic(query)
            .timeout(const Duration(seconds: 15));
        results = music
            .where((m) => m.contentType == type)
            .map(
              (m) => ListContentSelection(
                elementType: type,
                referenceId: m.spotifyId,
                title: m.name,
                subtitle: type == 'SONG' || type == 'ALBUM'
                    ? m.artistName
                    : 'Artista',
                imageUrl: m.imageUrl,
              ),
            )
            .toList();
      }
      if (!mounted || version != _version) return;
      final unique = <String, ListContentSelection>{
        for (final item in results) _key(item): item,
      };
      setState(() => _results = unique.values.toList());
    } catch (_) {
      if (mounted && version == _version) {
        setState(() => _searchError = 'No pudimos buscar. Intenta nuevamente.');
      }
    } finally {
      if (mounted && version == _version) setState(() => _searching = false);
    }
  }

  void _changeType(String type) {
    if (_saving || _type == type) return;
    setState(() => _type = type);
    _queryChanged(_query.text);
  }

  void _toggle(ListContentSelection item) {
    if (_saving) return;
    FocusScope.of(context).unfocus();
    final key = _key(item);
    if (!_selected.containsKey(key) && _selected.length >= 20) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Puedes elegir hasta 20 preferencias para comenzar.'),
        ),
      );
      return;
    }
    setState(() {
      if (_selected.containsKey(key)) {
        _selected.remove(key);
      } else {
        _selected[key] = item;
      }
    });
  }

  Future<void> _save() async {
    if (_saving || !_ready || _selected.isEmpty) return;
    _debounce?.cancel();
    FocusScope.of(context).unfocus();
    setState(() {
      _saving = true;
      _saveError = null;
    });
    try {
      await _preferences.save(
        _selected.values
            .map(
              (item) => {
                'elementType': item.elementType,
                'referenceId': item.referenceId,
              },
            )
            .toList(),
      );
      if (mounted) _continue();
    } catch (error) {
      if (mounted) {
        setState(
          () => _saveError = error is PreferencesOnboardingApiException
              ? error.message
              : 'No pudimos confirmar el guardado. Puedes reintentar sin duplicar tus preferencias.',
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _continue() =>
      Navigator.of(context)
          .pushNamedAndRemoveUntil(AppRoutes.home, (_) => false);

  @override
  void dispose() {
    _debounce?.cancel();
    _version++;
    _query.dispose();
    if (widget.booksApi == null) _books.close();
    if (widget.musicApi == null) _music.close();
    if (widget.preferencesApi == null) _preferences.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !_saving,
    child: Scaffold(
      backgroundColor: AppColors.cream,
      bottomNavigationBar: SafeArea(
        top: false,
        child: Center(
          heightFactor: 1,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 900),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_saveError != null) ...[
                    Text(_saveError!, style: AppTextStyles.body),
                    if (!_ready)
                      TextButton(
                        onPressed: _saving ? null : _prepare,
                        child: const Text('Reintentar perfil'),
                      ),
                    const SizedBox(height: 8),
                  ],
                  FilledButton(
                    onPressed: _saving || !_ready || _selected.isEmpty
                        ? null
                        : _save,
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Text(
                        _saving ? 'Guardando…' : 'Guardar y continuar',
                      ),
                    ),
                  ),
                  TextButton(
                    onPressed: _saving ? null : _continue,
                    child: const Text('Omitir por ahora'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 900),
            child: ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const Icon(
                  Icons.auto_awesome_outlined,
                  color: AppColors.sage,
                  size: 36,
                ),
                const SizedBox(height: 16),
                Text(
                  '¿Qué te gusta?',
                  style: AppTextStyles.pageTitle,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 12),
                Text(
                  'Elige libros, canciones, álbumes o artistas para empezar a conocerte. Puedes cambiar tus preferencias después en tu perfil.',
                  style: AppTextStyles.body,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    for (final entry in {
                      'BOOK': 'Libros',
                      'SONG': 'Canciones',
                      'ALBUM': 'Álbumes',
                      'ARTIST': 'Artistas',
                    }.entries)
                      ChoiceChip(
                        label: Text(entry.value),
                        selected: _type == entry.key,
                        selectedColor: AppColors.lilac,
                        onSelected: _saving
                            ? null
                            : (_) => _changeType(entry.key),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _query,
                  enabled: !_saving,
                  maxLength: 100,
                  onChanged: _queryChanged,
                  onSubmitted: (_) => _search(),
                  textInputAction: TextInputAction.search,
                  decoration: InputDecoration(
                    hintText:
                        'Buscar ${_type == 'BOOK'
                            ? 'libros o autores'
                            : _type == 'SONG'
                            ? 'canciones'
                            : _type == 'ALBUM'
                            ? 'álbumes'
                            : 'artistas'}',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: IconButton(
                      tooltip: 'Buscar preferencias',
                      onPressed: _saving ? null : _search,
                      icon: const Icon(Icons.arrow_forward),
                    ),
                  ),
                ),
                if (_selected.isNotEmpty) ...[
                  Text(
                    'Tu selección (${_selected.length}/20)',
                    style: AppTextStyles.cardTitle,
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _selected.values
                        .map(
                          (item) => InputChip(
                            label: Text(
                              item.title,
                              overflow: TextOverflow.ellipsis,
                            ),
                            avatar: Icon(
                              item.elementType == 'BOOK'
                                  ? Icons.menu_book_outlined
                                  : Icons.music_note_outlined,
                              size: 18,
                            ),
                            onDeleted: _saving ? null : () => _toggle(item),
                          ),
                        )
                        .toList(),
                  ),
                  const SizedBox(height: 16),
                ],
                if (_searching)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  )
                else if (_searchError != null)
                  Text(_searchError!, style: AppTextStyles.body)
                else if (_searched && _results.isEmpty)
                  Text(
                    'No encontramos resultados. Prueba otra búsqueda.',
                    style: AppTextStyles.secondary,
                  )
                else if (!_searched)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    child: Text(
                      'Busca algo que te encante y selecciona los resultados. No hay un mínimo obligatorio.',
                      style: AppTextStyles.secondary,
                    ),
                  ),
                for (final item in _results)
                  Card(
                    color: AppColors.warmWhite,
                    child: CheckboxListTile(
                      key: ValueKey(_key(item)),
                      value: _selected.containsKey(_key(item)),
                      onChanged: _saving ? null : (_) => _toggle(item),
                      title: Text(item.title),
                      subtitle: item.subtitle?.isNotEmpty == true
                          ? Text(item.subtitle!)
                          : null,
                      secondary: SizedBox(
                        width: 44,
                        height: 58,
                        child: item.imageUrl?.isNotEmpty == true
                            ? Image.network(
                                item.imageUrl!,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => _icon(item),
                              )
                            : _icon(item),
                      ),
                    ),
                  ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    ),
  );

  Widget _icon(ListContentSelection item) => ColoredBox(
    color: AppColors.lilac,
    child: Icon(
      item.elementType == 'BOOK'
          ? Icons.menu_book_outlined
          : Icons.music_note_outlined,
      color: AppColors.ink,
    ),
  );
}
