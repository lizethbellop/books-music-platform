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

class _MusicDetailPageState extends State<MusicDetailPage> {
  final MusicApiService _musicApiService = MusicApiService();
  final TextEditingController _reviewController =
      TextEditingController();

  MusicDetail? _detail;
  List<Map<String, dynamic>> _reviews = [];

  int? _reviewId;

  double _rating = 0;

  bool _isFavorite = false;
  bool _isLoading = true;
  bool _isSavingRating = false;
  bool _isSavingFavorite = false;
  bool _isSavingReview = false;

  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  @override
  void dispose() {
    _reviewController.dispose();
    super.dispose();
  }

  Future<void> _loadDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final detail = await _musicApiService.getMusicDetail(
        contentType: widget.item.contentType,
        spotifyId: widget.item.spotifyId,
      );

      final rating = await _musicApiService.getUserRating(
        userId: 1,
        spotifyId: detail.spotifyId,
        contentType: detail.contentType,
      );

      final isFavorite = await _musicApiService.getUserFavorite(
        userId: 1,
        spotifyId: detail.spotifyId,
        contentType: detail.contentType,
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
        _rating = rating;
        _isFavorite = isFavorite;
        _reviews = reviews;
        _reviewId = review?['id'] as int?;
        _reviewController.text =
            review?['reviewText'] as String? ?? '';
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _errorMessage = 'No se pudo cargar el detalle musical.';
        _isLoading = false;
      });
    }
  }

  Future<void> _refreshReviews() async {
    final detail = _detail;

    if (detail == null) return;

    try {
      final reviews = await _musicApiService.getReviews(
        detail: detail,
      );

      if (!mounted) return;

      setState(() {
        _reviews = reviews;
      });
    } catch (_) {
      // El detalle sigue disponible aunque falle
      // la actualización de reseñas.
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
        userId: 1,
        detail: detail,
        rating: newRating,
      );

      if (!mounted) return;

      _showMessage(
        'Calificación guardada',
      );
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
        userId: 1,
        detail: detail,
      );

      if (!mounted) return;

      setState(() {
        _isFavorite = true;
      });

      _showMessage(
        'Agregado a favoritos',
      );
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

  Future<void> _saveReview() async {
    final detail = _detail;
    final reviewText =
        _reviewController.text.trim();

    if (detail == null ||
        _isSavingReview) {
      return;
    }

    if (reviewText.isEmpty) {
      _showMessage(
        'Escribe una reseña primero',
      );
      return;
    }

    setState(() {
      _isSavingReview = true;
    });

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
          _reviewId =
              review?['id'] as int?;
        });

        _showMessage(
          'Reseña guardada',
        );
      } else {
        await _musicApiService.updateReview(
          reviewId: _reviewId!,
          reviewText: reviewText,
        );

        if (!mounted) return;

        _showMessage(
          'Reseña editada',
        );
      }

      await _refreshReviews();
    } catch (_) {
      if (!mounted) return;

      _showMessage(
        'No se pudo guardar la reseña',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSavingReview = false;
        });
      }
    }
  }

  Future<void> _deleteReview() async {
    final reviewId = _reviewId;

    if (reviewId == null ||
        _isSavingReview) {
      return;
    }

    setState(() {
      _isSavingReview = true;
    });

    try {
      await _musicApiService.deleteReview(
        reviewId: reviewId,
      );

      if (!mounted) return;

      setState(() {
        _reviewId = null;
        _reviewController.clear();
      });

      await _refreshReviews();

      if (!mounted) return;

      _showMessage(
        'Reseña eliminada',
      );
    } catch (_) {
      if (!mounted) return;

      _showMessage(
        'No se pudo eliminar la reseña',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSavingReview = false;
        });
      }
    }
  }

  void _showMessage(
    String message,
  ) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
      ),
    );
  }

  @override
  Widget build(
    BuildContext context,
  ) {
    return Scaffold(
      backgroundColor:
          AppColors.cream,
      appBar: AppBar(
        backgroundColor:
            AppColors.cream,
        foregroundColor:
            AppColors.ink,
        elevation: 0,
        title: Text(
          'Detalle musical',
          style:
              AppTextStyles.cardTitle,
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
        child:
            CircularProgressIndicator(
          color:
              AppColors.lavender,
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

    return LayoutBuilder(
      builder: (
        context,
        constraints,
      ) {
        final isDesktop =
            constraints.maxWidth >= 800;

        final information = Column(
          crossAxisAlignment:
              CrossAxisAlignment.start,
          children: [
            _buildInformation(
              detail,
            ),
            const SizedBox(
              height: 28,
            ),
            _buildRating(),
            const SizedBox(
              height: 18,
            ),
            _buildFavoriteButton(),
            const SizedBox(
              height: 36,
            ),
            _buildReviews(),
          ],
        );

        return SingleChildScrollView(
          padding:
              EdgeInsets.symmetric(
            horizontal:
                isDesktop
                    ? 48
                    : 20,
            vertical:
                isDesktop
                    ? 36
                    : 20,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints:
                  const BoxConstraints(
                maxWidth: 1000,
              ),
              child: isDesktop
                  ? Row(
                      crossAxisAlignment:
                          CrossAxisAlignment
                              .start,
                      children: [
                        SizedBox(
                          width: 360,
                          child:
                              _MusicImage(
                            imageUrl:
                                detail
                                    .imageUrl,
                          ),
                        ),
                        const SizedBox(
                          width: 48,
                        ),
                        Expanded(
                          child:
                              information,
                        ),
                      ],
                    )
                  : Column(
                      crossAxisAlignment:
                          CrossAxisAlignment
                              .stretch,
                      children: [
                        Center(
                          child:
                              ConstrainedBox(
                            constraints:
                                const BoxConstraints(
                              maxWidth:
                                  340,
                            ),
                            child:
                                _MusicImage(
                              imageUrl:
                                  detail
                                      .imageUrl,
                            ),
                          ),
                        ),
                        const SizedBox(
                          height: 28,
                        ),
                        information,
                      ],
                    ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildInformation(
    MusicDetail detail,
  ) {
    return Column(
      crossAxisAlignment:
          CrossAxisAlignment.start,
      children: [
        Text(
          detail.name,
          style:
              AppTextStyles.pageTitle
                  .copyWith(
            fontSize: 36,
          ),
        ),

        if (detail.artistName
                ?.isNotEmpty ==
            true) ...[
          const SizedBox(
            height: 8,
          ),
          Text(
            detail.artistName!,
            style:
                AppTextStyles
                    .sectionTitle,
          ),
        ],

        const SizedBox(
          height: 16,
        ),

        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            _InformationChip(
              icon: Icons
                  .library_music_outlined,
              text: _typeText(
                detail.contentType,
              ),
            ),

            if (detail.releaseDate !=
                null)
              _InformationChip(
                icon: Icons
                    .calendar_today_outlined,
                text:
                    detail.releaseDate!,
              ),

            if (detail.totalTracks !=
                null)
              _InformationChip(
                icon:
                    Icons.queue_music,
                text:
                    '${detail.totalTracks} canciones',
              ),
          ],
        ),
      ],
    );
  }

  Widget _buildRating() {
    return Container(
      width: double.infinity,
      padding:
          const EdgeInsets.all(
        20,
      ),
      decoration:
          BoxDecoration(
        color:
            AppColors.warmWhite,
        borderRadius:
            BorderRadius.circular(
          20,
        ),
        border:
            Border.all(
          color:
              AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment:
            CrossAxisAlignment.start,
        children: [
          Text(
            'Tu calificación',
            style:
                AppTextStyles
                    .sectionTitle,
          ),

          const SizedBox(
            height: 12,
          ),

          Wrap(
            spacing: 2,
            children:
                List.generate(
              5,
              (index) {
                final value =
                    (index + 1)
                        .toDouble();

                return IconButton(
                  tooltip:
                      '${index + 1} estrellas',
                  onPressed:
                      _isSavingRating
                          ? null
                          : () =>
                              _saveRating(
                                value,
                              ),
                  icon: Icon(
                    value <= _rating
                        ? Icons.star
                        : Icons
                            .star_border,
                    size: 34,
                    color:
                        AppColors
                            .butter,
                  ),
                );
              },
            ),
          ),

          if (_isSavingRating)
            Text(
              'Guardando calificación...',
              style:
                  AppTextStyles
                      .secondary,
            ),
        ],
      ),
    );
  }

  Widget _buildFavoriteButton() {
    return SizedBox(
      width: double.infinity,
      child:
          FilledButton.icon(
        onPressed:
            _isSavingFavorite ||
                    _isFavorite
                ? null
                : _addFavorite,
        style:
            FilledButton.styleFrom(
          backgroundColor:
              AppColors.lavender,
          foregroundColor:
              AppColors.ink,
        ),
        icon: Icon(
          _isFavorite
              ? Icons.favorite
              : Icons
                  .favorite_border,
        ),
        label: Text(
          _isSavingFavorite
              ? 'Guardando...'
              : _isFavorite
                  ? 'En favoritos'
                  : 'Agregar a favoritos',
        ),
      ),
    );
  }

  Widget _buildReviews() {
    return Column(
      crossAxisAlignment:
          CrossAxisAlignment.start,
      children: [
        Text(
          'Tu reseña',
          style:
              AppTextStyles
                  .sectionTitle,
        ),

        const SizedBox(
          height: 12,
        ),

        TextField(
          controller:
              _reviewController,
          maxLines: 5,
          decoration:
              const InputDecoration(
            hintText:
                'Escribe lo que piensas de este contenido...',
            border:
                OutlineInputBorder(),
          ),
        ),

        const SizedBox(
          height: 16,
        ),

        FilledButton(
          onPressed:
              _isSavingReview
                  ? null
                  : _saveReview,
          child: Text(
            _reviewId == null
                ? 'Publicar reseña'
                : 'Editar reseña',
          ),
        ),

        if (_reviewId != null) ...[
          const SizedBox(
            height: 12,
          ),
          OutlinedButton.icon(
            onPressed:
                _isSavingReview
                    ? null
                    : _deleteReview,
            icon:
                const Icon(
              Icons.delete_outline,
            ),
            label:
                const Text(
              'Eliminar reseña',
            ),
          ),
        ],

        const SizedBox(
          height: 36,
        ),

        Text(
          'Reseñas',
          style:
              AppTextStyles
                  .sectionTitle,
        ),

        const SizedBox(
          height: 16,
        ),

        if (_reviews.isEmpty)
          Text(
            'Todavía no hay reseñas para este contenido.',
            style:
                AppTextStyles
                    .secondary,
          )
        else
          for (final review
              in _reviews)
            Container(
              width:
                  double.infinity,
              margin:
                  const EdgeInsets
                      .only(
                bottom: 12,
              ),
              padding:
                  const EdgeInsets
                      .all(
                16,
              ),
              decoration:
                  BoxDecoration(
                color:
                    AppColors
                        .warmWhite,
                borderRadius:
                    BorderRadius
                        .circular(
                  12,
                ),
              ),
              child: Column(
                crossAxisAlignment:
                    CrossAxisAlignment
                        .start,
                children: [
                  Text(
                    'Usuario ${review['userId']}',
                    style:
                        AppTextStyles
                            .cardTitle,
                  ),
                  const SizedBox(
                    height: 8,
                  ),
                  Text(
                    '${review['reviewText'] ?? ''}',
                    style:
                        AppTextStyles
                            .secondary,
                  ),
                ],
              ),
            ),
      ],
    );
  }

  String _typeText(
    String type,
  ) {
    return switch (type) {
      'SONG' => 'Canción',
      'ALBUM' => 'Álbum',
      'ARTIST' => 'Artista',
      _ => type,
    };
  }
}

class _MusicImage extends StatelessWidget {
  final String? imageUrl;

  const _MusicImage({
    required this.imageUrl,
  });

  @override
  Widget build(
    BuildContext context,
  ) {
    final hasImage =
        imageUrl
                ?.trim()
                .isNotEmpty ==
            true;

    return AspectRatio(
      aspectRatio: 1,
      child: ClipRRect(
        borderRadius:
            BorderRadius.circular(
          20,
        ),
        child: hasImage
            ? Image.network(
                imageUrl!,
                fit: BoxFit.cover,
                errorBuilder:
                    (
                  context,
                  error,
                  stackTrace,
                ) =>
                        const _ImagePlaceholder(),
              )
            : const _ImagePlaceholder(),
      ),
    );
  }
}

class _ImagePlaceholder
    extends StatelessWidget {
  const _ImagePlaceholder();

  @override
  Widget build(
    BuildContext context,
  ) {
    return Container(
      color:
          AppColors.lilac,
      child:
          const Center(
        child: Icon(
          Icons.music_note,
          size: 80,
          color:
              AppColors.ink,
        ),
      ),
    );
  }
}

class _InformationChip
    extends StatelessWidget {
  final IconData icon;
  final String text;

  const _InformationChip({
    required this.icon,
    required this.text,
  });

  @override
  Widget build(
    BuildContext context,
  ) {
    return Chip(
      avatar: Icon(
        icon,
        size: 17,
        color:
            AppColors.ink,
      ),
      label: Text(text),
      backgroundColor:
          AppColors.mint,
      side:
          BorderSide.none,
    );
  }
}

class _ErrorView
    extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorView({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(
    BuildContext context,
  ) {
    return Center(
      child: Padding(
        padding:
            const EdgeInsets
                .all(
          24,
        ),
        child: Column(
          mainAxisSize:
              MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color:
                  AppColors.ink,
            ),
            const SizedBox(
              height: 16,
            ),
            Text(
              message,
              textAlign:
                  TextAlign.center,
              style:
                  AppTextStyles.body,
            ),
            const SizedBox(
              height: 20,
            ),
            FilledButton(
              onPressed:
                  onRetry,
              child:
                  const Text(
                'Reintentar',
              ),
            ),
          ],
        ),
      ),
    );
  }
}