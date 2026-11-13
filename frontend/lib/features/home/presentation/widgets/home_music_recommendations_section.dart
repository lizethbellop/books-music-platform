import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../music/presentation/pages/music_detail_page.dart';
import '../../data/models/home_music_recommendations.dart';
import '../../data/services/recommendations_api_service.dart';

class HomeMusicRecommendationsSection extends StatefulWidget {
  final String userId;
  final bool allowRefresh;
  final String? referenceId;
  final MusicRecommendationKind kind;
  final VoidCallback onOpenLists;
  final RecommendationsApiService? api;

  const HomeMusicRecommendationsSection({
    super.key,
    required this.userId,
    required this.kind,
    required this.onOpenLists,
    this.api,
    this.allowRefresh = true,
    this.referenceId,
  });

  @override
  State<HomeMusicRecommendationsSection> createState() =>
      _HomeMusicRecommendationsSectionState();
}

class _HomeMusicRecommendationsSectionState
    extends State<HomeMusicRecommendationsSection> {
  late RecommendationsApiService _api;
  late Future<HomeMusicRecommendations> _future;
  bool _requestInFlight = false;
  int _requestGeneration = 0;

  String? _lastFingerprint;
  String? _refreshMessage;

  Future<HomeMusicRecommendations> _load({bool refreshed = false}) {
    final generation = ++_requestGeneration;
    _requestInFlight = true;
    _refreshMessage = null;
    return _api
        .getHomeMusic(widget.kind, referenceId: widget.referenceId)
        .then((data) {
          if (generation == _requestGeneration) {
            final items = data.items
                .map(
                  (item) =>
                      '${item.item.spotifyId}:${item.item.name}:${item.reason}',
                )
                .join('|');
            final fingerprint = '${data.mode}:${data.preparationStatus}:$items';
            if (refreshed) {
              _refreshMessage = fingerprint == _lastFingerprint
                  ? 'Actualizado. No hay nuevas sugerencias por ahora.'
                  : 'Recomendaciones actualizadas.';
            }
            _lastFingerprint = fingerprint;
          }
          return data;
        })
        .whenComplete(() {
          if (generation == _requestGeneration) _requestInFlight = false;
        });
  }

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? RecommendationsApiService();
    _future = _load();
  }

  @override
  void didUpdateWidget(covariant HomeMusicRecommendationsSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.api != widget.api) {
      if (oldWidget.api == null) _api.close();
      _api = widget.api ?? RecommendationsApiService();
    }
    if (oldWidget.api != widget.api ||
        oldWidget.userId != widget.userId ||
        oldWidget.kind != widget.kind ||
        oldWidget.referenceId != widget.referenceId) {
      _future = _load();
    }
  }

  void _refresh() {
    if (_requestInFlight) return;
    _api.retryMissingMusicImages();
    final future = _load(refreshed: true);
    setState(() {
      _future = future;
    });
  }

  @override
  void dispose() {
    if (widget.api == null) _api.close();
    super.dispose();
  }

  String _title(bool personalized) => widget.referenceId != null
      ? switch (widget.kind) {
          MusicRecommendationKind.artists => 'Artistas similares',
          MusicRecommendationKind.albums => 'Álbumes similares',
          MusicRecommendationKind.songs => 'Canciones similares',
        }
      : switch (widget.kind) {
          MusicRecommendationKind.artists =>
            personalized ? 'Artistas para ti' : 'Artistas por descubrir',
          MusicRecommendationKind.albums =>
            personalized ? 'Álbumes para ti' : 'Álbumes por descubrir',
          MusicRecommendationKind.songs =>
            personalized ? 'Tu siguiente canción' : 'Canciones por descubrir',
        };

  IconData get _icon => switch (widget.kind) {
    MusicRecommendationKind.artists => Icons.person_outline,
    MusicRecommendationKind.albums => Icons.album_outlined,
    MusicRecommendationKind.songs => Icons.music_note_outlined,
  };

  Future<void> _open(RecommendedMusic recommendation) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) =>
            MusicDetailPage(userId: widget.userId, item: recommendation.item),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<HomeMusicRecommendations>(
    future: _future,
    builder: (context, snapshot) {
      final data = snapshot.data;
      final personalized =
          data?.mode == 'knn_content' && data!.items.isNotEmpty;
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  _title(personalized),
                  style: AppTextStyles.sectionTitle,
                ),
              ),
              if (widget.referenceId == null && widget.allowRefresh)
                IconButton(
                  tooltip: snapshot.connectionState == ConnectionState.done
                      ? 'Actualizar recomendaciones musicales'
                      : 'Actualizando recomendaciones musicales',
                  onPressed: snapshot.connectionState == ConnectionState.done
                      ? _refresh
                      : null,
                  icon: snapshot.connectionState == ConnectionState.done
                      ? const Icon(Icons.refresh)
                      : const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          if (_refreshMessage != null && widget.referenceId == null) ...[
            Text(_refreshMessage!, style: AppTextStyles.secondary),
            const SizedBox(height: 12),
          ],
          if (data?.isPreparing == true) ...[
            Text(
              widget.referenceId == null
                  ? 'Estamos preparando más opciones musicales a partir de tus gustos.'
                  : 'Estamos preparando opciones relacionadas con este elemento.',
              style: AppTextStyles.secondary,
            ),
            const SizedBox(height: 12),
          ],
          if (data?.preparationStatus == 'failed' ||
              data?.preparationStatus == 'busy') ...[
            Text(
              'Algunas opciones siguen pendientes; puedes volver a consultar más tarde.',
              style: AppTextStyles.secondary,
            ),
            const SizedBox(height: 12),
          ],
          if (snapshot.connectionState != ConnectionState.done && data == null)
            const Padding(
              padding: EdgeInsets.all(28),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (snapshot.hasError)
            _message(
              'No pudimos cargar estas sugerencias.',
              widget.referenceId == null && widget.allowRefresh
                  ? 'Reintentar'
                  : null,
              widget.referenceId == null && widget.allowRefresh
                  ? _refresh
                  : null,
            )
          else if (data!.items.isEmpty)
            _message(
              data.isPreparing
                  ? 'Estamos buscando opciones relacionadas. Puedes seguir explorando mientras se preparan.'
                  : widget.referenceId != null &&
                        data.mode == 'reference_not_prepared'
                  ? 'Por ahora no hay similares disponibles para este elemento.'
                  : data.mode == 'catalog_not_ready'
                  ? 'Todavía no hay sugerencias disponibles en esta categoría.'
                  : widget.referenceId != null
                  ? 'Aún no encontramos otras opciones con suficientes características en común.'
                  : 'Aún no encontramos más opciones para tus gustos. Puedes ampliar tus preferencias o volver después.',
              widget.referenceId == null && widget.allowRefresh
                  ? 'Mis listas'
                  : null,
              widget.referenceId == null && widget.allowRefresh
                  ? widget.onOpenLists
                  : null,
            )
          else ...[
            Text(
              widget.referenceId != null
                  ? 'Opciones que comparten características con el elemento que estás viendo.'
                  : personalized
                  ? 'A partir de tus preferencias, favoritos y valoraciones.'
                  : 'Explora y guarda lo que te guste para recibir sugerencias más cercanas a ti.',
              style: AppTextStyles.secondary,
            ),
            if (data.partial && widget.referenceId == null) ...[
              const SizedBox(height: 6),
              Text(
                'Estas sugerencias todavía no incluyen todos tus gustos.',
                style: AppTextStyles.secondary,
              ),
            ],
            const SizedBox(height: 16),
            SizedBox(
              height: 336,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: data.items.length,
                separatorBuilder: (_, _) => const SizedBox(width: 16),
                itemBuilder: (_, index) => _card(data.items[index]),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Datos musicales: MusicBrainz · Imágenes: Spotify / Cover Art Archive',
              style: AppTextStyles.secondary,
            ),
          ],
        ],
      );
    },
  );

  Widget _message(String message, [String? action, VoidCallback? onTap]) =>
      Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppColors.warmWhite,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message, style: AppTextStyles.body),
            if (action != null && onTap != null)
              TextButton(onPressed: onTap, child: Text(action)),
          ],
        ),
      );

  Widget _card(RecommendedMusic recommendation) {
    final item = recommendation.item;
    final artist = widget.kind == MusicRecommendationKind.artists;
    Widget placeholder() => Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.lilac, AppColors.mint],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Center(child: Icon(_icon, size: 52, color: AppColors.ink)),
    );
    return SizedBox(
      width: 180,
      child: Card(
        color: AppColors.warmWhite,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => _open(recommendation),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(artist ? 90 : 10),
                  child: SizedBox(
                    width: 156,
                    height: 156,
                    child: FutureBuilder<String?>(
                      future: _api.getMusicImage(recommendation),
                      builder: (context, snapshot) {
                        final imageUrl = snapshot.data;
                        return imageUrl == null
                            ? placeholder()
                            : Image.network(
                                imageUrl,
                                fit: BoxFit.cover,
                                excludeFromSemantics: true,
                                errorBuilder: (_, _, _) => placeholder(),
                              );
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  item.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.cardTitle,
                ),
                if (!artist && item.artistName != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    item.artistName!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTextStyles.secondary,
                  ),
                ],
                const SizedBox(height: 10),
                Text(
                  recommendation.reason,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.secondary,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
