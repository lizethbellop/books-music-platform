import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/models/book_search_item.dart';
import '../../data/services/books_api_service.dart';
import '../widgets/book_filter_chip.dart';
import 'book_detail_page.dart';

class BooksHomePage extends StatefulWidget {
  const BooksHomePage({super.key});

  @override
  State<BooksHomePage> createState() => _BooksHomePageState();
}



class _BooksHomePageState extends State<BooksHomePage> {
  final TextEditingController _searchController =
      TextEditingController();

  final BooksApiService _booksApiService =
      BooksApiService();

  static const String _testUserId =
      '550e8400-e29b-41d4-a716-446655440000';

  List<BookSearchItem> _results = [];
  List<BookSearchItem> _libraryResults = [];
  List<BookSearchItem> _favoriteResults = [];

  List<BookSearchItem> _exploreBooks = [];

  bool _isLoading = false;
  bool _hasSearched = false;

  String? _errorMessage;

  String _selectedFilter = 'Todos';

  @override
  void initState() {
    super.initState();
    _loadExploreBooks();
  }

  Future<void> _searchBooks() async {
    final query = _searchController.text.trim();

    if (query.isEmpty) {
      return;
    }

    setState(() {
      _selectedFilter = 'Todos';
      _isLoading = true;
      _hasSearched = true;
      _errorMessage = null;
    });

    try {
      final results =
          await _booksApiService.searchBooks(query);

      if (!mounted) return;

      setState(() {
        _results = results;
      });
    } catch (e) {
      if (!mounted) return;

      debugPrint(
        'ERROR AL BUSCAR LIBROS: $e',
      );

      setState(() {
        _results = [];
        _errorMessage =
            'No se pudo realizar la búsqueda.';
      });
    }

    if (!mounted) return;

    setState(() {
      _isLoading = false;
    });
  }

Future<void> _loadExploreBooks() async {
  setState(() {
    _isLoading = true;
    _errorMessage = null;
  });

  try {
    final books =
        await _booksApiService.getExploreBooks();

    if (!mounted) return;

    setState(() {
      _exploreBooks = books;
    });
  } catch (e) {
    if (!mounted) return;

    debugPrint(
      'ERROR AL CARGAR EXPLORAR: $e',
    );

    setState(() {
      _errorMessage =
          'No se pudo cargar el catálogo de libros.';
    });
  }

  if (!mounted) return;

  setState(() {
    _isLoading = false;
  });
}

  Future<void> _selectFilter(
    String filter,
  ) async {
    setState(() {
      _selectedFilter = filter;
      _errorMessage = null;
    });

    if (filter == 'Todos') {
      setState(() {
        _isLoading = false;
      });

      return;
    }

    await _loadSelectedFilter();
  }

  Future<void> _loadSelectedFilter() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      if (_selectedFilter == 'Favoritos') {
        final favorites =
            await _booksApiService.getFavorites(
          _testUserId,
        );

        if (!mounted) return;

        setState(() {
          _favoriteResults = favorites
              .map(
                (favorite) => BookSearchItem(
                  externalId:
                      favorite.externalId,
                  title: favorite.title,
                  authors:
                      favorite.author == null
                          ? []
                          : [
                              favorite.author!,
                            ],
                  coverUrl:
                      favorite.coverUrl,
                ),
              )
              .toList();
        });
      } else {
        final library =
            await _booksApiService.getLibrary(
          _testUserId,
        );

        if (!mounted) return;

        String status;

        switch (_selectedFilter) {
          case 'Leyendo':
            status = 'LEYENDO';
            break;

          case 'Por leer':
            status = 'POR_LEER';
            break;

          case 'Leídos':
            status = 'LEIDO';
            break;

          default:
            status = '';
        }

        setState(() {
          _libraryResults = library
              .where(
                (book) =>
                    book.readingStatus ==
                    status,
              )
              .map(
                (book) => BookSearchItem(
                  externalId:
                      book.externalId,
                  title: book.title,
                  authors:
                      book.author == null
                          ? []
                          : [
                              book.author!,
                            ],
                  coverUrl:
                      book.coverUrl,
                ),
              )
              .toList();
        });
      }
    } catch (e) {
      if (!mounted) return;

      debugPrint(
        'ERROR AL CARGAR FILTRO: $e',
      );

      setState(() {
        _errorMessage =
            'No se pudo cargar esta sección.';
      });
    }

    if (!mounted) return;

    setState(() {
      _isLoading = false;
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment:
                CrossAxisAlignment.start,
            children: [
              Text(
                'Libros',
                style: AppTextStyles.pageTitle,
              ),

              const SizedBox(height: 24),

              TextField(
                controller: _searchController,
                textInputAction:
                    TextInputAction.search,
                onSubmitted: (_) {
                  _searchBooks();
                },
                decoration: InputDecoration(
                  hintText:
                      'Buscar libros o autores...',
                  prefixIcon: const Icon(
                    Icons.search,
                    color: AppColors.ink,
                  ),
                  suffixIcon: IconButton(
                    onPressed: _searchBooks,
                    icon: const Icon(
                      Icons.arrow_forward,
                      color: AppColors.ink,
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 18),

              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  BookFilterChip(
                    label: 'Todos',
                    selected:
                        _selectedFilter ==
                        'Todos',
                    onTap: () {
                      _selectFilter(
                        'Todos',
                      );
                    },
                  ),

                  BookFilterChip(
                    label: 'Leyendo',
                    selected:
                        _selectedFilter ==
                        'Leyendo',
                    onTap: () {
                      _selectFilter(
                        'Leyendo',
                      );
                    },
                  ),

                  BookFilterChip(
                    label: 'Por leer',
                    selected:
                        _selectedFilter ==
                        'Por leer',
                    onTap: () {
                      _selectFilter(
                        'Por leer',
                      );
                    },
                  ),

                  BookFilterChip(
                    label: 'Leídos',
                    selected:
                        _selectedFilter ==
                        'Leídos',
                    onTap: () {
                      _selectFilter(
                        'Leídos',
                      );
                    },
                  ),

                  BookFilterChip(
                    label: 'Favoritos',
                    selected:
                        _selectedFilter ==
                        'Favoritos',
                    onTap: () {
                      _selectFilter(
                        'Favoritos',
                      );
                    },
                  ),
                ],
              ),

              const SizedBox(height: 30),

              _buildContent(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(
            vertical: 30,
          ),
          child:
              CircularProgressIndicator(),
        ),
      );
    }

    if (_errorMessage != null) {
      return Text(
        _errorMessage!,
        style: AppTextStyles.secondary,
      );
    }

    if (_selectedFilter == 'Leyendo' ||
        _selectedFilter == 'Por leer' ||
        _selectedFilter == 'Leídos') {
      if (_libraryResults.isEmpty) {
        return Text(
          'No tienes libros en esta sección.',
          style:
              AppTextStyles.secondary,
        );
      }

      return _buildBookGrid(
        _libraryResults,
        _selectedFilter,
      );
    }

    if (_selectedFilter == 'Favoritos') {
      if (_favoriteResults.isEmpty) {
        return Text(
          'Todavía no tienes libros favoritos.',
          style:
              AppTextStyles.secondary,
        );
      }

      return _buildBookGrid(
        _favoriteResults,
        'Favoritos',
      );
    }

    if (!_hasSearched) {
      if (_exploreBooks.isEmpty) {
        return Text(
          'No hay libros disponibles para explorar.',
          style: AppTextStyles.secondary,
        );
      }

      return _buildBookGrid(
        _exploreBooks,
        'Explorar libros',
      );
    }

    if (_results.isEmpty) {
      return Text(
        'No se encontraron resultados.',
        style:
            AppTextStyles.secondary,
      );
    }

    return _buildBookGrid(
      _results,
      'Resultados',
    );
  }

  Widget _buildBookGrid(
    List<BookSearchItem> books,
    String title,
  ) {
    return Column(
      crossAxisAlignment:
          CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style:
              AppTextStyles.sectionTitle,
        ),

        const SizedBox(height: 16),

        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: books
              .map(
                (book) =>
                    _BookResultCard(
                  book: book,
                ),
              )
              .toList(),
        ),
      ],
    );
  }
}

class _BookResultCard
    extends StatelessWidget {
  final BookSearchItem book;

  const _BookResultCard({
    required this.book,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 210,
      child: Card(
        clipBehavior:
            Clip.antiAlias,
        child: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) =>
                    BookDetailPage(
                  externalId:
                      book.externalId,
                ),
              ),
            );
          },
          child: Padding(
            padding:
                const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment:
                  CrossAxisAlignment.start,
              children: [
                AspectRatio(
                  aspectRatio: 2 / 3,
                  child:
                      book.coverUrl !=
                                  null &&
                              book.coverUrl!
                                  .isNotEmpty
                          ? Image.network(
                              book.coverUrl!,
                              fit:
                                  BoxFit.cover,
                              errorBuilder: (
                                context,
                                error,
                                stackTrace,
                              ) {
                                return _placeholder();
                              },
                            )
                          : _placeholder(),
                ),

                const SizedBox(
                  height: 12,
                ),

                Text(
                  book.title,
                  maxLines: 2,
                  overflow:
                      TextOverflow
                          .ellipsis,
                  style:
                      AppTextStyles
                          .cardTitle,
                ),

                const SizedBox(
                  height: 4,
                ),

                Text(
                  book.authors.isEmpty
                      ? 'Autor desconocido'
                      : book.authors
                          .join(', '),
                  maxLines: 2,
                  overflow:
                      TextOverflow
                          .ellipsis,
                  style:
                      AppTextStyles
                          .secondary,
                ),

                if (book
                        .firstPublishYear !=
                    null) ...[
                  const SizedBox(
                    height: 4,
                  ),
                  Text(
                    '${book.firstPublishYear}',
                    style:
                        AppTextStyles
                            .secondary,
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _placeholder() {
    return Container(
      color: AppColors.lilac,
      child: const Center(
        child: Icon(
          Icons.menu_book_outlined,
          size: 44,
          color: AppColors.ink,
        ),
      ),
    );
  }
}