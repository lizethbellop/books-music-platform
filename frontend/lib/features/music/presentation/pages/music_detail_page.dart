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

  MusicDetail? _detail;
  bool _isLoading = true;
  String? _errorMessage;

  double _rating = 0;
  bool _isFavorite = false;

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

      if (!mounted) return;

      setState(() {
        _detail = detail;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _errorMessage = 'No se pudo cargar el detalle musical.';
        _isLoading = false;
      });
    }
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
                          borderRadius: BorderRadius.circular(18),
                          child: Image.network(
                            detail.imageUrl!,
                            fit: BoxFit.cover,
                          ),
                        )
                      : Container(
                          decoration: BoxDecoration(
                            color: AppColors.lilac,
                            borderRadius: BorderRadius.circular(18),
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
                      final newRating = value.toDouble();

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

                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Calificación guardada'),
                          ),
                        );
                      } catch (e) {
                        if (!mounted) return;

                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('No se pudo guardar la calificación'),
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

                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Agregado a favoritos'),
                      ),
                    );
                  } catch (e) {
                    if (!mounted) return;

                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('No se pudo agregar a favoritos'),
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