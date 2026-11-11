import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../books/presentation/pages/book_detail_page.dart';
import '../../data/models/home_book_recommendations.dart';
import '../../data/services/recommendations_api_service.dart';

class HomeBookRecommendationsSection extends StatefulWidget {
  final String userId;
  final VoidCallback onOpenLists;
  final RecommendationsApiService? api;

  const HomeBookRecommendationsSection({
    super.key,
    required this.userId,
    required this.onOpenLists,
    this.api,
  });

  @override
  State<HomeBookRecommendationsSection> createState() =>
      _HomeBookRecommendationsSectionState();
}

class _HomeBookRecommendationsSectionState
    extends State<HomeBookRecommendationsSection> {
  late RecommendationsApiService _api;
  late Future<HomeBookRecommendations> _future;

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? RecommendationsApiService();
    _future = _api.getHomeBooks();
  }

  @override
  void didUpdateWidget(covariant HomeBookRecommendationsSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.api != widget.api) {
      if (oldWidget.api == null) _api.close();
      _api = widget.api ?? RecommendationsApiService();
    }
    if (oldWidget.userId != widget.userId || oldWidget.api != widget.api) {
      _future = _api.getHomeBooks();
    }
  }

  void _refresh() {
    final future = _api.getHomeBooks();
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
    if (mounted) _refresh();
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Row(
        children: [
          Expanded(
            child: Text('Lecturas para ti', style: AppTextStyles.sectionTitle),
          ),
          IconButton(
            tooltip: 'Actualizar recomendaciones',
            onPressed: _refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      const SizedBox(height: 8),
      FutureBuilder<HomeBookRecommendations>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
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
              action: 'Reintentar',
              onTap: _refresh,
            );
          }
          final data = snapshot.data!;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                data.seedCount > 0
                    ? 'Inspiradas en tus preferencias y en tus listas.'
                    : 'Descubre una lectura. Agrega libros a tus preferencias o listas para recibir sugerencias más cercanas a ti.',
                style: AppTextStyles.secondary,
              ),
              const SizedBox(height: 12),
              TextButton.icon(
                onPressed: widget.onOpenLists,
                icon: const Icon(Icons.collections_bookmark_outlined),
                label: const Text('Mis listas'),
              ),
              if (data.partial)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(
                    'Algunos libros no se pudieron consultar. Puedes actualizar para intentarlo de nuevo.',
                    style: AppTextStyles.secondary,
                  ),
                ),
              if (data.items.isEmpty)
                _message(
                  'Todavía no encontramos otras lecturas para mostrarte. Prueba agregando libros a tus preferencias o listas.',
                  action: 'Ver mis listas',
                  onTap: widget.onOpenLists,
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

  Widget _message(
    String message, {
    required String action,
    required VoidCallback onTap,
  }) => Container(
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
        const SizedBox(height: 12),
        OutlinedButton(onPressed: onTap, child: Text(action)),
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
