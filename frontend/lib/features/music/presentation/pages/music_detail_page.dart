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

  MusicDetail? _detail;

  bool _isLoading = true;
  bool _isSavingRating = false;
  bool _isSavingFavorite = false;
  bool _isFavorite = false;

  double _rating = 0;
  String? _errorMessage;

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

      if (!mounted) return;

      setState(() {
        _detail = detail;
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
        _isFavorite = true;
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

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
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

    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop =
            constraints.maxWidth >= 800;

        return SingleChildScrollView(
          padding: EdgeInsets.symmetric(
            horizontal: isDesktop ? 48 : 20,
            vertical: isDesktop ? 36 : 20,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: 1000,
              ),
              child: isDesktop
                  ? Row(
                      crossAxisAlignment:
                          CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          width: 360,
                          child: _MusicImage(
                            imageUrl: detail.imageUrl,
                          ),
                        ),
                        const SizedBox(width: 48),
                        Expanded(
                          child: _DetailInformation(
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
            ),
            icon: isSavingFavorite
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child:
                        CircularProgressIndicator(
                      strokeWidth: 2,
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

  const _ErrorView({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color: AppColors.ink,
            ),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: AppTextStyles.body,
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: onRetry,
              style: FilledButton.styleFrom(
                backgroundColor:
                    AppColors.lavender,
                foregroundColor: AppColors.ink,
              ),
              child: const Text(
                'Volver a intentar',
              ),
            ),
          ],
        ),
      ),
    );
  }
}