import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/models/book_detail.dart';
import '../../data/models/review.dart';
import '../../data/services/books_api_service.dart';

class BookDetailPage extends StatefulWidget {
  final String externalId;
  final String userId;

  const BookDetailPage({
    super.key,
    required this.externalId,
    required this.userId,
  });

  @override
  State<BookDetailPage> createState() => _BookDetailPageState();
}

class _BookDetailPageState extends State<BookDetailPage> {
  final BooksApiService _booksApiService = BooksApiService();

  final TextEditingController _reviewController = TextEditingController();

  BookDetail? _book;

  bool _isLoading = true;
  bool _isLoadingReviews = false;

  String? _errorMessage;
  String? _selectedReadingStatus;

  bool _isFavorite = false;

  double? _selectedRating;

  List<Review> _reviews = [];

  @override
  void initState() {
    super.initState();

    _loadBook();
    _loadReviews();
    _loadUserBookState();
  }

  @override
  void dispose() {
    _reviewController.dispose();
    super.dispose();
  }

  Review? get _currentUserReview {
    for (final review in _reviews) {
      if (review.userId == widget.userId) {
        return review;
      }
    }

    return null;
  }

  Future<void> _toggleFavorite() async {
    try {
      if (_isFavorite) {
        await _booksApiService.removeFavorite(
          externalId: widget.externalId,
          userId: widget.userId,
        );
      } else {
        await _booksApiService.addFavorite(
          externalId: widget.externalId,
          userId: widget.userId,
        );
      }

      if (!mounted) return;

      setState(() {
        _isFavorite = !_isFavorite;
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            _isFavorite
                ? 'Libro agregado a favoritos'
                : 'Libro eliminado de favoritos',
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo actualizar favoritos')),
      );
    }
  }

  Widget _buildReadingStatusButton({
    required String label,
    required String status,
  }) {
    final isSelected = _selectedReadingStatus == status;

    return InkWell(
      onTap: () {
        _updateReadingStatus(status);
      },
      borderRadius: BorderRadius.circular(999),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.ink
              : AppColors.lilac.withValues(alpha: 0.45),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          label,
          style: AppTextStyles.button.copyWith(
            color: isSelected ? AppColors.warmWhite : AppColors.ink,
          ),
        ),
      ),
    );
  }

  Future<void> _loadUserBookState() async {
    try {
      final library = await _booksApiService.getLibrary(widget.userId);

      final favorites = await _booksApiService.getFavorites(widget.userId);

      if (!mounted) return;

      String? readingStatus;

      for (final userBook in library) {
        if (userBook.externalId == widget.externalId) {
          readingStatus = userBook.readingStatus;
          break;
        }
      }

      bool isFavorite = false;

      for (final favorite in favorites) {
        if (favorite.externalId == widget.externalId) {
          isFavorite = true;
          break;
        }
      }

      setState(() {
        _selectedReadingStatus = readingStatus;

        _isFavorite = isFavorite;
      });
    } catch (e) {
      // No bloqueamos la pantalla
      // si estas consultas fallan.
    }
  }

  Future<void> _loadBook() async {
    try {
      final book = await _booksApiService.getBookDetail(widget.externalId);

      if (!mounted) return;

      setState(() {
        _book = book;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _errorMessage = 'No se pudo cargar la información del libro.';

        _isLoading = false;
      });
    }
  }

  Future<void> _loadReviews() async {
    setState(() {
      _isLoadingReviews = true;
    });

    try {
      final reviews = await _booksApiService.getReviews(widget.externalId);

      if (!mounted) return;

      Review? currentUserReview;

      for (final review in reviews) {
        if (review.userId == widget.userId) {
          currentUserReview = review;
          break;
        }
      }

      setState(() {
        _reviews = reviews;

        _isLoadingReviews = false;

        if (currentUserReview != null) {
          _selectedRating = currentUserReview.rating;

          _reviewController.text = currentUserReview.reviewText ?? '';
        }
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _isLoadingReviews = false;
      });
    }
  }

  Future<void> _updateReadingStatus(String status) async {
    try {
      await _booksApiService.updateReadingStatus(
        externalId: widget.externalId,
        userId: widget.userId,
        status: status,
      );

      if (!mounted) return;

      setState(() {
        _selectedReadingStatus = status;
      });

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Estado de lectura actualizado')),
      );
    } catch (e) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('No se pudo actualizar el estado de lectura'),
        ),
      );
    }
  }

  Future<void> _rateBook(double rating) async {
    try {
      await _booksApiService.rateBook(
        externalId: widget.externalId,
        userId: widget.userId,
        rating: rating,
      );

      if (!mounted) return;

      setState(() {
        _selectedRating = rating;
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Calificación guardada: $rating estrellas')),
      );
    } catch (e) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo guardar la calificación')),
      );
    }
  }

  Future<void> _saveReview() async {
    if (_selectedRating == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Selecciona una calificación antes de guardar la reseña',
          ),
        ),
      );

      return;
    }

    final reviewText = _reviewController.text.trim();

    final isCreating = _currentUserReview == null;

    try {
      if (isCreating) {
        await _booksApiService.createReview(
          externalId: widget.externalId,
          userId: widget.userId,
          rating: _selectedRating!,
          reviewText: reviewText.isEmpty ? null : reviewText,
        );
      } else {
        await _booksApiService.updateReview(
          externalId: widget.externalId,
          userId: widget.userId,
          rating: _selectedRating!,
          reviewText: reviewText.isEmpty ? null : reviewText,
        );
      }

      await _loadReviews();

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(isCreating ? 'Reseña creada' : 'Reseña actualizada'),
        ),
      );
    } catch (e) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo guardar la reseña')),
      );
    }
  }

  Future<void> _deleteReview() async {
    try {
      await _booksApiService.deleteReview(
        externalId: widget.externalId,
        userId: widget.userId,
      );

      if (!mounted) return;

      _reviewController.clear();

      setState(() {
        _selectedRating = null;
      });

      await _loadReviews();

      if (!mounted) return;

      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Reseña eliminada')));
    } catch (e) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo eliminar la reseña')),
      );
    }
  }

  Widget _buildRatingStar(int index) {
    final starNumber = index + 1;

    final currentRating = _selectedRating ?? 0.0;

    IconData icon;

    if (currentRating >= starNumber) {
      icon = Icons.star;
    } else if (currentRating >= starNumber - 0.5) {
      icon = Icons.star_half;
    } else {
      icon = Icons.star_border;
    }

    return SizedBox(
      width: 44,
      height: 44,
      child: Stack(
        children: [
          Center(child: Icon(icon, size: 36, color: AppColors.ink)),
          Positioned(
            left: 0,
            top: 0,
            bottom: 0,
            width: 22,
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                _rateBook(starNumber - 0.5);
              },
            ),
          ),
          Positioned(
            right: 0,
            top: 0,
            bottom: 0,
            width: 22,
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                _rateBook(starNumber.toDouble());
              },
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      appBar: AppBar(
        backgroundColor: AppColors.cream,
        foregroundColor: AppColors.ink,
        elevation: 0,
        title: const Text('Detalle del libro'),
      ),
      body: _buildContent(),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Text(_errorMessage!, style: AppTextStyles.secondary),
      );
    }

    if (_book == null) {
      return Center(
        child: Text(
          'No se encontró información del libro.',
          style: AppTextStyles.secondary,
        ),
      );
    }

    final book = _book!;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 28),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1200),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              LayoutBuilder(
                builder: (context, constraints) {
                  final isDesktop = constraints.maxWidth >= 760;

                  if (isDesktop) {
                    return Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildCover(book),

                        const SizedBox(width: 42),

                        Expanded(child: _buildBookHeaderInfo(book)),
                      ],
                    );
                  }

                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Center(child: _buildCover(book)),

                      const SizedBox(height: 28),

                      _buildBookHeaderInfo(book),
                    ],
                  );
                },
              ),

              const SizedBox(height: 44),

              const Divider(color: AppColors.border),

              const SizedBox(height: 34),

              _buildRatingSection(),

              const SizedBox(height: 38),

              _buildReviewEditor(),

              const SizedBox(height: 44),

              _buildDescriptionSection(book),

              if (book.subjects.isNotEmpty) ...[
                const SizedBox(height: 38),

                _buildSubjectsSection(book),
              ],

              const SizedBox(height: 44),

              _buildReviewsSection(),

              const SizedBox(height: 50),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCover(BookDetail book) {
    return Container(
      width: 235,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            blurRadius: 18,
            offset: Offset(0, 8),
            color: Color(0x18000000),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: AspectRatio(
        aspectRatio: 2 / 3,
        child: book.coverUrl != null && book.coverUrl!.isNotEmpty
            ? Image.network(
                book.coverUrl!,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return _placeholder();
                },
              )
            : _placeholder(),
      ),
    );
  }

  Widget _buildBookHeaderInfo(BookDetail book) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(book.title, style: AppTextStyles.pageTitle),

        const SizedBox(height: 10),

        Text(
          book.authors.isEmpty ? 'Autor desconocido' : book.authors.join(', '),
          style: AppTextStyles.body,
        ),

        if (book.firstPublishDate != null) ...[
          const SizedBox(height: 8),

          Text(
            'Primera publicación: ${book.firstPublishDate}',
            style: AppTextStyles.secondary,
          ),
        ],

        const SizedBox(height: 30),

        Text('Estado de lectura', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 14),

        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            _buildReadingStatusButton(label: 'Por leer', status: 'POR_LEER'),
            _buildReadingStatusButton(label: 'Leyendo', status: 'LEYENDO'),
            _buildReadingStatusButton(label: 'Leído', status: 'LEIDO'),
          ],
        ),

        const SizedBox(height: 18),

        OutlinedButton.icon(
          onPressed: _toggleFavorite,
          icon: Icon(
            _isFavorite ? Icons.favorite : Icons.favorite_border,
            color: AppColors.ink,
          ),
          label: Text(
            _isFavorite ? 'Quitar de favoritos' : 'Agregar a favoritos',
          ),
        ),
      ],
    );
  }

  Widget _buildRatingSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Calificación', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 8),

        Text(
          _selectedRating == null
              ? 'Selecciona una calificación'
              : 'Tu calificación: $_selectedRating / 5',
          style: AppTextStyles.secondary,
        ),

        const SizedBox(height: 10),

        Wrap(children: List.generate(5, (index) => _buildRatingStar(index))),
      ],
    );
  }

  Widget _buildReviewEditor() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Tu reseña', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 14),

        TextField(
          controller: _reviewController,
          minLines: 4,
          maxLines: 6,
          decoration: const InputDecoration(
            hintText: 'Escribe tu opinión sobre este libro...',
            border: OutlineInputBorder(),
          ),
        ),

        const SizedBox(height: 14),

        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            ElevatedButton.icon(
              onPressed: _saveReview,
              icon: const Icon(Icons.save_outlined),
              label: Text(
                _currentUserReview == null
                    ? 'Publicar reseña'
                    : 'Actualizar reseña',
              ),
            ),

            if (_currentUserReview != null)
              OutlinedButton.icon(
                onPressed: _deleteReview,
                icon: const Icon(Icons.delete_outline),
                label: const Text('Eliminar reseña'),
              ),
          ],
        ),
      ],
    );
  }

  Widget _buildDescriptionSection(BookDetail book) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Descripción', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 14),

        Text(
          book.description?.isNotEmpty == true
              ? book.description!
              : 'No hay descripción disponible.',
          style: AppTextStyles.body,
        ),
      ],
    );
  }

  Widget _buildSubjectsSection(BookDetail book) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Temas', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 14),

        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: book.subjects
              .take(8)
              .map(
                (subject) => Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.lilac.withValues(alpha: 0.45),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    subject,
                    style: AppTextStyles.secondary.copyWith(
                      color: AppColors.ink,
                    ),
                  ),
                ),
              )
              .toList(),
        ),
      ],
    );
  }

  Widget _buildReviewsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Reseñas', style: AppTextStyles.sectionTitle),

        const SizedBox(height: 14),

        if (_isLoadingReviews)
          const Center(child: CircularProgressIndicator())
        else if (_reviews.isEmpty)
          Text(
            'Todavía no hay reseñas para este libro.',
            style: AppTextStyles.secondary,
          )
        else
          Column(
            children: _reviews.map((review) {
              return Container(
                width: double.infinity,
                margin: const EdgeInsets.only(bottom: 14),
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: AppColors.warmWhite,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.star, size: 18, color: AppColors.ink),

                        const SizedBox(width: 6),

                        Text('${review.rating} / 5', style: AppTextStyles.body),
                      ],
                    ),

                    const SizedBox(height: 10),

                    Text(review.username, style: AppTextStyles.cardTitle),
                    const SizedBox(height: 8),
                    Text(
                      review.reviewText?.isNotEmpty == true
                          ? review.reviewText!
                          : 'Sin comentario.',
                      style: AppTextStyles.body,
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
      ],
    );
  }

  Widget _placeholder() {
    return Container(
      color: AppColors.lilac,
      child: const Center(
        child: Icon(Icons.menu_book_outlined, size: 60, color: AppColors.ink),
      ),
    );
  }
}
