import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/models/music_detail.dart';
import '../../data/models/music_search_item.dart';
import '../../data/services/music_api_service.dart';

class MusicDetailPage extends StatefulWidget {
  final MusicSearchItem item;

  const MusicDetailPage({
    super.key,
    required this.item,
  });

  @override
  State<MusicDetailPage> createState() =>
      _MusicDetailPageState();
}

class _MusicDetailPageState
    extends State<MusicDetailPage> {
  final MusicApiService _musicApiService =
      MusicApiService();

  final TextEditingController _reviewController =
      TextEditingController();

  MusicDetail? _detail;

  bool _isLoading = true;
  bool _isSavingRating = false;
  bool _isSavingFavorite = false;
  bool _isFavorite = false;

  double _rating = 0;
  String? _errorMessage;

  int? _reviewId;

  List<Map<String, dynamic>> _reviews = [];

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  Future<void> _loadDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final detail =
          await _musicApiService.getMusicDetail(
        contentType: widget.item.contentType,
        spotifyId: widget.item.spotifyId,
      );

      final review = await _musicApiService.getUserReview(
        userId: 1,
        detail: detail,
      );

      final reviews = await _musicApiService.getReviews(
        detail: detail,
      );

      if (!mounted) return;

      setState(() {
        _detail = detail;
        _reviews = reviews;

        if (review != null) {
          _reviewId = review['id'];
          _reviewController.text =
              review['reviewText'] ?? '';
        } else {
          _reviewId = null;
          _reviewController.clear();
        }

        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _errorMessage =
            'No se pudo cargar el detalle musical.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _saveRating(
    double newRating,
  ) async {
    final detail = _detail;

    if (detail == null || _isSavingRating) {
      return;
    }

    final previousRating = _rating;

    setState(() {
      _rating = newRating;
      _isSavingRating = true;
    });

    try {
      await _musicApiService.rateMusic(
        // Temporal: Música todavía utiliza Long.
        userId: 1,
        detail: detail,
        rating: newRating,
      );

      if (!mounted) return;

      _showMessage('Calificación guardada');
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _rating = previousRating;
      });

      _showMessage(
        'No se pudo guardar la calificación',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSavingRating = false;
        });
      }
    }
  }

  Future<void> _addFavorite() async {
    final detail = _detail;

    if (detail == null ||
        _isSavingFavorite ||
        _isFavorite) {
      return;
    }

    setState(() {
      _isSavingFavorite = true;
    });

    try {
      await _musicApiService.addFavorite(
        // Temporal: Música todavía utiliza Long.
        userId: 1,
        detail: detail,
      );

      if (!mounted) return;

      setState(() {
        _errorMessage =
            'No se pudo cargar el detalle musical.';
        _isLoading = false;
      });

      _showMessage('Agregado a favoritos');
    } catch (_) {
      if (!mounted) return;

      _showMessage(
        'No se pudo agregar a favoritos',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSavingFavorite = false;
        });
      }
    }
  }

  Future<void> _refreshReviews() async {
    final detail = _detail;

    if (detail == null) {
      return;
    }

    try {
      final reviews = await _musicApiService.getReviews(
        detail: detail,
      );

      if (!mounted) return;

      setState(() {
        _reviews = reviews;
      });
    } catch (e) {
      // No bloqueamos la pantalla si solamente falla
      // la actualización de la lista de reseñas.
    }
  }

  @override
  void dispose() {
    _reviewController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      appBar: AppBar(
        backgroundColor: AppColors.cream,
        foregroundColor: AppColors.ink,
        elevation: 0,
        title: Text(
          'Detalle musical',
          style: AppTextStyles.cardTitle,
        ),
      ),
      body: SafeArea(
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(
          color: AppColors.lavender,
        ),
      );
    }

    if (_errorMessage != null) {
      return _ErrorView(
        message: _errorMessage!,
        onRetry: _loadDetail,
      );
    }

    final detail = _detail!;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(40),
      child: Center(
        child: SizedBox(
          width: 700,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: SizedBox(
                  width: 320,
                  height: 320,
                  child: detail.imageUrl != null &&
                          detail.imageUrl!.isNotEmpty
                      ? ClipRRect(
                          borderRadius:
                              BorderRadius.circular(18),
                          child: Image.network(
                            detail.imageUrl!,
                            fit: BoxFit.cover,
                          ),
                        )
                      : Container(
                          decoration: BoxDecoration(
                            color: AppColors.lilac,
                            borderRadius:
                                BorderRadius.circular(18),
                          ),
                        ),
                      ],
                    )
                  : Column(
                      crossAxisAlignment:
                          CrossAxisAlignment.stretch,
                      children: [
                        Center(
                          child: ConstrainedBox(
                            constraints:
                                const BoxConstraints(
                              maxWidth: 340,
                            ),
                            child: _MusicImage(
                              imageUrl:
                                  detail.imageUrl,
                            ),
                          ),
                        ),
                        const SizedBox(height: 28),
                        _DetailInformation(
                          detail: detail,
                          rating: _rating,
                          isFavorite: _isFavorite,
                          isSavingRating:
                              _isSavingRating,
                          isSavingFavorite:
                              _isSavingFavorite,
                          onRatingSelected:
                              _saveRating,
                          onFavoritePressed:
                              _addFavorite,
                        ),
                      ],
                    ),
            ),
          ),
        );
      },
    );
  }
}

class _MusicImage extends StatelessWidget {
  final String? imageUrl;

  const _MusicImage({
    required this.imageUrl,
  });

  @override
  Widget build(BuildContext context) {
    final hasImage =
        imageUrl?.trim().isNotEmpty == true;

    return AspectRatio(
      aspectRatio: 1,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: hasImage
            ? Image.network(
                imageUrl!,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) {
                  return const _ImagePlaceholder();
                },
              )
            : const _ImagePlaceholder(),
      ),
    );
  }
}

class _ImagePlaceholder extends StatelessWidget {
  const _ImagePlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.lilac,
      child: const Center(
        child: Icon(
          Icons.music_note,
          size: 80,
          color: AppColors.ink,
        ),
      ),
    );
  }
}

class _DetailInformation extends StatelessWidget {
  final MusicDetail detail;
  final double rating;
  final bool isFavorite;
  final bool isSavingRating;
  final bool isSavingFavorite;
  final ValueChanged<double> onRatingSelected;
  final VoidCallback onFavoritePressed;

  const _DetailInformation({
    required this.detail,
    required this.rating,
    required this.isFavorite,
    required this.isSavingRating,
    required this.isSavingFavorite,
    required this.onRatingSelected,
    required this.onFavoritePressed,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment:
          CrossAxisAlignment.start,
      children: [
        Text(
          detail.name,
          style: AppTextStyles.pageTitle.copyWith(
            fontSize: 36,
          ),
        ),

        if (detail.artistName?.isNotEmpty == true) ...[
          const SizedBox(height: 8),
          Text(
            detail.artistName!,
            style: AppTextStyles.sectionTitle,
          ),
        ],

        const SizedBox(height: 16),

        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            _InformationChip(
              icon: Icons.library_music_outlined,
              text: _typeText(
                detail.contentType,
              ),
            ),
            if (detail.releaseDate != null)
              _InformationChip(
                icon: Icons.calendar_today_outlined,
                text: detail.releaseDate!,
              ),
            if (detail.totalTracks != null)
              _InformationChip(
                icon: Icons.queue_music,
                text:
                    '${detail.totalTracks} canciones',
              ),
          ],
        ),

        const SizedBox(height: 32),

        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.warmWhite,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: AppColors.border,
            ),
          ),
          child: Column(
            crossAxisAlignment:
                CrossAxisAlignment.start,
            children: [
              Text(
                'Tu calificación',
                style: AppTextStyles.sectionTitle
                    .copyWith(
                  fontSize: 22,
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 2,
                children: List.generate(
                  5,
                  (index) {
                    final value =
                        (index + 1).toDouble();

                    return IconButton(
                      tooltip:
                          '${index + 1} estrellas',
                      onPressed: isSavingRating
                          ? null
                          : () {
                              onRatingSelected(
                                value,
                              );
                            },
                      icon: Icon(
                        value <= rating
                            ? Icons.star
                            : Icons.star_border,
                        size: 34,
                        color: AppColors.butter,
                      ),
                    );
                  },
                ),
              ),
              if (isSavingRating) ...[
                const SizedBox(height: 8),
                Text(
                  'Guardando calificación...',
                  style: AppTextStyles.secondary,
                ),
              ],
            ],
          ),
        ),

        const SizedBox(height: 20),

        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed:
                isSavingFavorite || isFavorite
                    ? null
                    : onFavoritePressed,
            style: FilledButton.styleFrom(
              backgroundColor:
                  AppColors.lavender,
              foregroundColor: AppColors.ink,
              disabledBackgroundColor:
                  AppColors.lilac,
              disabledForegroundColor:
                  AppColors.ink,
              padding: const EdgeInsets.symmetric(
                horizontal: 24,
                vertical: 18,
              ),

              const SizedBox(height: 10),

              Row(
                children: List.generate(5, (index) {
                  final value = index + 1;

                  return IconButton(
                    onPressed: () async {
                      final newRating =
                          value.toDouble();

                      setState(() {
                        _rating = newRating;
                      });

                      try {
                        await _musicApiService.rateMusic(
                          userId: 1,
                          detail: detail,
                          rating: newRating,
                        );

                        if (!mounted) return;

                        ScaffoldMessenger.of(context)
                            .showSnackBar(
                          const SnackBar(
                            content: Text(
                              'Calificación guardada',
                            ),
                          ),
                        );
                      } catch (e) {
                        if (!mounted) return;

                        ScaffoldMessenger.of(context)
                            .showSnackBar(
                          const SnackBar(
                            content: Text(
                              'No se pudo guardar la calificación',
                            ),
                          ),
                        );
                      }
                    },
                    icon: Icon(
                      value <= _rating
                          ? Icons.star
                          : Icons.star_border,
                      size: 34,
                      color: AppColors.ink,
                    ),
                  )
                : Icon(
                    isFavorite
                        ? Icons.favorite
                        : Icons.favorite_border,
                  ),
            label: Text(
              isSavingFavorite
                  ? 'Guardando...'
                  : isFavorite
                      ? 'En favoritos'
                      : 'Agregar a favoritos',
              style: AppTextStyles.button,
            ),
          ),
        ),
      ],
    );
  }

  String _typeText(String type) {
    return switch (type) {
      'SONG' => 'Canción',
      'ALBUM' => 'Álbum',
      'ARTIST' => 'Artista',
      _ => type,
    };
  }
}

class _InformationChip extends StatelessWidget {
  final IconData icon;
  final String text;

  const _InformationChip({
    required this.icon,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(
        icon,
        size: 17,
        color: AppColors.ink,
      ),
      label: Text(text),
      backgroundColor: AppColors.mint,
      side: BorderSide.none,
    );
  }
}

class _ErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

                    ScaffoldMessenger.of(context)
                        .showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Agregado a favoritos',
                        ),
                      ),
                    );
                  } catch (e) {
                    if (!mounted) return;

                    ScaffoldMessenger.of(context)
                        .showSnackBar(
                      const SnackBar(
                        content: Text(
                          'No se pudo agregar a favoritos',
                        ),
                      ),
                    );
                  }
                },
                icon: Icon(
                  _isFavorite
                      ? Icons.favorite
                      : Icons.favorite_border,
                ),
                label: Text(
                  _isFavorite
                      ? 'En favoritos'
                      : 'Agregar a favoritos',
                ),
              ),

              const SizedBox(height: 30),

              Text(
                'Tu reseña',
                style: AppTextStyles.sectionTitle,
              ),

              const SizedBox(height: 12),

              TextField(
                controller: _reviewController,
                maxLines: 5,
                decoration: const InputDecoration(
                  hintText:
                      'Escribe lo que piensas de este contenido...',
                  border: OutlineInputBorder(),
                ),
              ),

              const SizedBox(height: 16),

              FilledButton(
                onPressed: () async {
                  final reviewText =
                      _reviewController.text.trim();

                  if (reviewText.isEmpty) {
                    ScaffoldMessenger.of(context)
                        .showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Escribe una reseña primero',
                        ),
                      ),
                    );
                    return;
                  }

                  try {
                    if (_reviewId == null) {
                      await _musicApiService.createReview(
                        userId: 1,
                        detail: detail,
                        reviewText: reviewText,
                      );

                      final review =
                          await _musicApiService.getUserReview(
                        userId: 1,
                        detail: detail,
                      );

                      if (!mounted) return;

                      setState(() {
                        _reviewId = review?['id'];
                      });

                      await _refreshReviews();

                      if (!mounted) return;

                      ScaffoldMessenger.of(context)
                          .showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Reseña guardada',
                          ),
                        ),
                      );
                    } else {
                      await _musicApiService.updateReview(
                        reviewId: _reviewId!,
                        reviewText: reviewText,
                      );

                      await _refreshReviews();

                      if (!mounted) return;

                      ScaffoldMessenger.of(context)
                          .showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Reseña editada',
                          ),
                        ),
                      );
                    }
                  } catch (e) {
                    if (!mounted) return;

                    ScaffoldMessenger.of(context)
                        .showSnackBar(
                      const SnackBar(
                        content: Text(
                          'No se pudo guardar la reseña',
                        ),
                      ),
                    );
                  }
                },
                child: Text(
                  _reviewId == null
                      ? 'Publicar reseña'
                      : 'Editar reseña',
                ),
              ),

              if (_reviewId != null) ...[
                const SizedBox(height: 12),

                OutlinedButton.icon(
                  onPressed: () async {
                    try {
                      await _musicApiService.deleteReview(
                        reviewId: _reviewId!,
                      );

                      if (!mounted) return;

                      setState(() {
                        _reviewId = null;
                        _reviewController.clear();
                      });

                      await _refreshReviews();

                      if (!mounted) return;

                      ScaffoldMessenger.of(context)
                          .showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Reseña eliminada',
                          ),
                        ),
                      );
                    } catch (e) {
                      if (!mounted) return;

                      ScaffoldMessenger.of(context)
                          .showSnackBar(
                        const SnackBar(
                          content: Text(
                            'No se pudo eliminar la reseña',
                          ),
                        ),
                      );
                    }
                  },
                  icon: const Icon(
                    Icons.delete_outline,
                  ),
                  label: const Text(
                    'Eliminar reseña',
                  ),
                ),
              ],

              const SizedBox(height: 40),

              Text(
                'Reseñas',
                style: AppTextStyles.sectionTitle,
              ),

              const SizedBox(height: 16),

              if (_reviews.isEmpty)
                Text(
                  'Todavía no hay reseñas para este contenido.',
                  style: AppTextStyles.secondary,
                )
              else
                ..._reviews.map(
                  (review) => Container(
                    width: double.infinity,
                    margin: const EdgeInsets.only(
                      bottom: 12,
                    ),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.warmWhite,
                      borderRadius:
                          BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment:
                          CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Usuario ${review['userId']}',
                          style:
                              AppTextStyles.sectionTitle,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          review['reviewText'] ?? '',
                          style:
                              AppTextStyles.secondary,
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  String _typeText(String type) {
    switch (type) {
      case 'SONG':
        return 'Canción';

      case 'ALBUM':
        return 'Álbum';

      case 'ARTIST':
        return 'Artista';

      default:
        return type;
    }
  }
}