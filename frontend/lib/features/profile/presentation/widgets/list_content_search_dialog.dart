import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../books/data/services/books_api_service.dart';
import '../../../music/data/services/music_api_service.dart';

class ListContentSelection {
  final String elementType;
  final String referenceId;
  final String title;
  final String? subtitle;
  final String? imageUrl;

  const ListContentSelection({
    required this.elementType,
    required this.referenceId,
    required this.title,
    this.subtitle,
    this.imageUrl,
  });
}

class ListContentSearchDialog extends StatefulWidget {
  final BooksApiService? booksApi;
  final MusicApiService? musicApi;

  const ListContentSearchDialog({
    super.key,
    this.booksApi,
    this.musicApi,
  });

  @override
  State<ListContentSearchDialog> createState() =>
      _ListContentSearchDialogState();
}

class _ListContentSearchDialogState
    extends State<ListContentSearchDialog> {
  final _queryController = TextEditingController();
  late final _booksApi = widget.booksApi ?? BooksApiService();
late final _musicApi = widget.musicApi ?? MusicApiService();

  String _elementType = 'BOOK';
  List<ListContentSelection> _results = [];

  bool _isLoading = false;
  bool _hasSearched = false;
  String? _errorMessage;
  int _searchVersion = 0;

  @override
  void dispose() {
    _queryController.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final query = _queryController.text.trim();

    if (query.isEmpty) {
      setState(() {
        _errorMessage = 'Escribe algo para buscar.';
      });
      return;
    }

    final version = ++_searchVersion;
    final type = _elementType;

    setState(() {
      _isLoading = true;
      _hasSearched = true;
      _errorMessage = null;
      _results = [];
    });

    try {
      late final List<ListContentSelection> results;

      if (type == 'BOOK') {
        final books = await _booksApi.searchBooks(query);

        results = books.map((book) {
          return ListContentSelection(
            elementType: 'BOOK',
            referenceId: book.externalId,
            title: book.title,
            subtitle: book.authors.isEmpty
                ? null
                : book.authors.join(', '),
            imageUrl: book.coverUrl,
          );
        }).toList();
      } else {
        final music = await _musicApi.searchMusic(query);

        results = music
            .where((item) => item.contentType == type)
            .map((item) {
          return ListContentSelection(
            elementType: type,
            referenceId: item.spotifyId,
            title: item.name,
            subtitle: type == 'SONG'
                ? item.artistName
                : 'Artista',
            imageUrl: item.imageUrl,
          );
        }).toList();
      }

      if (!mounted || version != _searchVersion) return;

      setState(() {
        _results = results;
      });
    } catch (_) {
      if (!mounted || version != _searchVersion) return;

      setState(() {
        _errorMessage =
            'No se pudo realizar la búsqueda. Intenta nuevamente.';
      });
    } finally {
      if (mounted && version == _searchVersion) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _changeType(String type) {
    if (_elementType == type) return;

    setState(() {
      _searchVersion++;
      _elementType = type;
      _results = [];
      _hasSearched = false;
      _isLoading = false;
      _errorMessage = null;
    });

    if (_queryController.text.trim().isNotEmpty) {
      _search();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.warmWhite,
      title: Text(
        'Agregar contenido',
        style: AppTextStyles.sectionTitle,
      ),
      content: SizedBox(
        width: 520,
        height: MediaQuery.sizeOf(context).height * 0.6,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 8,
              children: [
                _typeChip('BOOK', 'Libros'),
                _typeChip('SONG', 'Canciones'),
                _typeChip('ARTIST', 'Artistas'),
              ],
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _queryController,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _search(),
              decoration: InputDecoration(
                hintText: 'Buscar...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  onPressed: _isLoading ? null : _search,
                  icon: const Icon(Icons.arrow_forward),
                  tooltip: 'Buscar',
                ),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(child: _buildResults()),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
      ],
    );
  }

  Widget _typeChip(String type, String label) {
    return ChoiceChip(
      label: Text(label),
      selected: _elementType == type,
      selectedColor: AppColors.lilac,
      onSelected: (_) => _changeType(type),
    );
  }

  Widget _buildResults() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(child: Text(_errorMessage!));
    }

    if (!_hasSearched) {
      return const Center(
        child: Text('Busca el contenido que quieras agregar.'),
      );
    }

    if (_results.isEmpty) {
      return const Center(
        child: Text('No se encontraron resultados.'),
      );
    }

    return ListView.builder(
      itemCount: _results.length,
      itemBuilder: (context, index) {
        final item = _results[index];

        return ListTile(
          contentPadding: EdgeInsets.zero,
          leading: _buildImage(item),
          title: Text(
            item.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          subtitle: item.subtitle == null
              ? null
              : Text(
                  item.subtitle!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
          trailing: IconButton(
            tooltip: 'Agregar a la lista',
            icon: const Icon(Icons.add_circle_outline),
            onPressed: () => Navigator.pop(context, item),
          ),
          onTap: () => Navigator.pop(context, item),
        );
      },
    );
  }

  Widget _buildImage(ListContentSelection item) {
    final url = item.imageUrl;

    Widget placeholder() {
      return ColoredBox(
        color: AppColors.lilac,
        child: Center(
          child: Icon(
            switch (item.elementType) {
              'BOOK' => Icons.book_outlined,
              'ARTIST' => Icons.person_outline,
              _ => Icons.music_note_outlined,
            },
            color: AppColors.ink,
          ),
        ),
      );
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(
        item.elementType == 'ARTIST' ? 24 : 6,
      ),
      child: SizedBox(
        width: 48,
        height: 48,
        child: url == null || url.trim().isEmpty
            ? placeholder()
            : Image.network(
                url,
                fit: BoxFit.cover,
                errorBuilder: (_, error, stackTrace) => placeholder(),
              ),
      ),
    );
  }
}