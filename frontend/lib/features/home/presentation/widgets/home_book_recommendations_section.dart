import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../books/presentation/pages/book_detail_page.dart';
import '../../data/models/home_book_recommendations.dart';
import '../../data/services/recommendations_api_service.dart';

class HomeBookRecommendationsSection extends StatefulWidget {
  final String userId;
  final bool allowRefresh;
  final String? referenceBookId;
  final VoidCallback onOpenLists;
  final RecommendationsApiService? api;

  const HomeBookRecommendationsSection({
    super.key,
    required this.userId,
    required this.onOpenLists,
    this.api,
    this.allowRefresh = true,
    this.referenceBookId,
  });

  @override
  State<HomeBookRecommendationsSection> createState() =>
      _HomeBookRecommendationsSectionState();
}

class _HomeBookRecommendationsSectionState
    extends State<HomeBookRecommendationsSection> {
  late RecommendationsApiService _api;
  late Future<HomeBookRecommendations> _future;
  bool _requestInFlight = false;
  int _requestGeneration = 0;

  String? _lastFingerprint;
  String? _refreshMessage;

  Future<HomeBookRecommendations> _load({bool refreshed = false}) {
    final generation = ++_requestGeneration;
    _requestInFlight = true;
    _refreshMessage = null;
    return _api
        .getHomeBooks(referenceBookId: widget.referenceBookId)
        .then((data) {
          if (generation == _requestGeneration) {
            final items = data.items
                .map(
                  (item) =>
                      '${item.book.externalId}:${item.book.title}:${item.reason}',
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
  void didUpdateWidget(covariant HomeBookRecommendationsSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.api != widget.api) {
      if (oldWidget.api == null) _api.close();
      _api = widget.api ?? RecommendationsApiService();
    }
    if (oldWidget.userId != widget.userId ||
        oldWidget.api != widget.api ||
        oldWidget.referenceBookId != widget.referenceBookId) {
      _future = _load();
    }
  }

  void _refresh() {
    if (_requestInFlight) return;
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

  Future<void> _openBook(RecommendedBook item) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => BookDetailPage(
          userId: widget.userId,
          externalId: item.book.externalId,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Row(
        children: [
          Expanded(
            child: FutureBuilder<HomeBookRecommendations>(
              future: _future,
              builder: (_, snapshot) => Text(
                widget.referenceBookId != null
                    ? 'Más libros como este'
                    : snapshot.data?.mode == 'explore'
                    ? 'Lecturas por descubrir'
                    : 'Lecturas para ti',
                style: AppTextStyles.sectionTitle,
              ),
            ),
          ),
          if (widget.referenceBookId == null && widget.allowRefresh)
            FutureBuilder<HomeBookRecommendations>(
              future: _future,
              builder: (_, snapshot) {
                final busy = snapshot.connectionState != ConnectionState.done;
                return IconButton(
                  tooltip: busy
                      ? 'Actualizando recomendaciones'
                      : 'Actualizar recomendaciones',
                  onPressed: busy ? null : _refresh,
                  icon: busy
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.refresh),
                );
              },
            ),
        ],
      ),
      const SizedBox(height: 8),
      FutureBuilder<HomeBookRecommendations>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done &&
              snapshot.data == null) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 48),
              child: Center(
                child: CircularProgressIndicator(
                  semanticsLabel: 'Cargando recomendaciones de libros',
                ),
              ),
            );
          }
          if (snapshot.hasError) {
            final error = snapshot.error;
            return _message(
              error is RecommendationsApiException ? error.message : 'No pudimos cargar tus recomendaciones. Intenta nuevamente.',
              action: widget.referenceBookId == null && widget.allowRefresh
                  ? 'Reintentar'
                  : null,
              onTap: widget.referenceBookId == null && widget.allowRefresh
                  ? _refresh
                  : null,
            );
          }
          final data = snapshot.data!;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_refreshMessage != null && widget.referenceBookId == null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(_refreshMessage!, style: AppTextStyles.secondary),
                ),
              Text(
                widget.referenceBookId != null
                    ? 'Lecturas que comparten temas, géneros u otras características con este libro.'
                    : data.seedCount > 0
                    ? 'Inspiradas en tus preferencias, listas y libros mejor valorados.'
                    : 'Descubre una lectura. Agrega libros a tus preferencias o listas para recibir sugerencias más cercanas a ti.',
                style: AppTextStyles.secondary,
              ),
              if (data.isPreparing) ...[
                const SizedBox(height: 12),
                Text(
                  widget.referenceBookId == null
                      ? 'Estamos preparando más opciones a partir de tus gustos.'
                      : 'Estamos preparando opciones relacionadas con este libro.',
                  style: AppTextStyles.secondary,
                ),
              ],
              if (data.preparationStatus == 'failed' ||
                  data.preparationStatus == 'busy') ...[
                const SizedBox(height: 12),
                Text(
                  'Algunas opciones siguen pendientes. Puedes volver a consultar más tarde.',
                  style: AppTextStyles.secondary,
                ),
              ],
              const SizedBox(height: 12),
              if (widget.referenceBookId == null && widget.allowRefresh)
                TextButton.icon(
                  onPressed: widget.onOpenLists,
                  icon: const Icon(Icons.collections_bookmark_outlined),
                  label: const Text('Mis listas'),
                ),
              if (data.partial && widget.referenceBookId == null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(
                    'Estas sugerencias todavía no incluyen todos tus gustos.',
                    style: AppTextStyles.secondary,
                  ),
                ),
              if (data.items.isEmpty)
                _message(
                  data.isPreparing
                      ? 'Estamos buscando libros con características similares. Puedes seguir explorando mientras se preparan.'
                      : data.mode == 'reference_not_prepared'
                      ? 'Por ahora no hay libros similares disponibles.'
                      : data.mode == 'catalog_not_ready'
                      ? 'Todavía no hay sugerencias de lectura disponibles.'
                      : widget.referenceBookId != null
                      ? 'Todavía no encontramos otros libros con suficientes características en común.'
                      : 'Aún no encontramos otras lecturas para tus gustos. Puedes ampliar tus preferencias o volver después.',
                  action: widget.referenceBookId == null && widget.allowRefresh
                      ? 'Ver mis listas'
                      : null,
                  onTap: widget.referenceBookId == null && widget.allowRefresh
                      ? widget.onOpenLists
                      : null,
                )
              else
                SizedBox(
                  height: 410,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: data.items.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 16),
                    itemBuilder: (context, index) => _RecommendationCard(
                      item: data.items[index],
                      onTap: () => _openBook(data.items[index]),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    ],
  );

  Widget _message(String message, {String? action, VoidCallback? onTap}) =>
      Container(
        width: double.infinity,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: AppColors.warmWhite,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message, style: AppTextStyles.body),
            if (action != null && onTap != null) ...[
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onTap, child: Text(action)),
            ],
          ],
        ),
      );
}

class _RecommendationCard extends StatelessWidget {
  final RecommendedBook item;
  final VoidCallback onTap;
  const _RecommendationCard({required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) => SizedBox(
    width: 180,
    child: Card(
      color: AppColors.warmWhite,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: 218,
                width: double.infinity,
                child: item.book.coverUrl?.isNotEmpty == true
                    ? Image.network(
                        item.book.coverUrl!,
                        fit: BoxFit.cover,
                        excludeFromSemantics: true,
                        errorBuilder: (_, _, _) => _placeholder(),
                      )
                    : _placeholder(),
              ),
              const SizedBox(height: 12),
              Text(
                item.book.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.cardTitle,
              ),
              const SizedBox(height: 6),
              Text(
                item.book.authors.join(', '),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.secondary,
              ),
              const SizedBox(height: 10),
              Text(
                item.reason,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.secondary.copyWith(color: AppColors.ink),
              ),
            ],
          ),
        ),
      ),
    ),
  );

  Widget _placeholder() => Container(
    color: AppColors.lilac,
    child: const Icon(Icons.menu_book_outlined, size: 42, color: AppColors.ink),
  );
}
