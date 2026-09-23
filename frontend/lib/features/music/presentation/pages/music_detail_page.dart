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
  State<MusicDetailPage> createState() => _MusicDetailPageState();
}

class _MusicDetailPageState extends State<MusicDetailPage> {
  final MusicApiService _musicApiService = MusicApiService();

  final TextEditingController _reviewController =
      TextEditingController();

  MusicDetail? _detail;

  bool _isLoading = true;
  String? _errorMessage;

  double _rating = 0;
  bool _isFavorite = false;

  int? _reviewId;

  List<Map<String, dynamic>> _reviews = [];

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  Future<void> _loadDetail() async {
    try {
      final detail = await _musicApiService.getMusicDetail(
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
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _errorMessage =
            'No se pudo cargar el detalle musical.';
        _isLoading = false;
      });
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
        title: const Text('Detalle musical'),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    if (_errorMessage != null) {
      return Center(
        child: Text(
          _errorMessage!,
          style: AppTextStyles.secondary,
        ),
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
                          child: const Icon(
                            Icons.music_note,
                            size: 80,
                            color: AppColors.ink,
                          ),
                        ),
                ),
              ),

              const SizedBox(height: 30),

              Text(
                detail.name,
                style: AppTextStyles.pageTitle,
              ),

              const SizedBox(height: 8),

              Text(
                detail.artistName ?? '',
                style: AppTextStyles.sectionTitle,
              ),

              const SizedBox(height: 16),

              Text(
                _typeText(detail.contentType),
                style: AppTextStyles.secondary,
              ),

              if (detail.releaseDate != null) ...[
                const SizedBox(height: 8),
                Text(
                  'Fecha de lanzamiento: ${detail.releaseDate}',
                  style: AppTextStyles.secondary,
                ),
              ],

              if (detail.totalTracks != null) ...[
                const SizedBox(height: 8),
                Text(
                  'Número de canciones: ${detail.totalTracks}',
                  style: AppTextStyles.secondary,
                ),
              ],

              const SizedBox(height: 30),

              Text(
                'Tu calificación',
                style: AppTextStyles.sectionTitle,
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
                  );
                }),
              ),

              const SizedBox(height: 20),

              FilledButton.icon(
                onPressed: () async {
                  try {
                    await _musicApiService.addFavorite(
                      userId: 1,
                      detail: detail,
                    );

                    if (!mounted) return;

                    setState(() {
                      _isFavorite = true;
                    });

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