import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';

import '../../data/models/music_search_item.dart';
import '../../data/music_item.dart';
import '../../data/services/music_api_service.dart';
import '../../../home/presentation/widgets/home_music_recommendations_section.dart';
import '../../../home/data/models/home_music_recommendations.dart';
import '../../../home/data/services/recommendations_api_service.dart';
import '../widgets/music_card.dart';
import '../widgets/music_filter_chip.dart';
import 'music_detail_page.dart';

class MusicHomePage extends StatefulWidget {
  final String userId;
  final RecommendationsApiService? recommendationsApi;
  final MusicApiService? musicApi;

  const MusicHomePage({
    super.key,
    required this.userId,
    this.recommendationsApi,
    this.musicApi,
  });

  @override
  State<MusicHomePage> createState() => _MusicHomePageState();
}

class _MusicHomePageState extends State<MusicHomePage> {
  final TextEditingController _searchController = TextEditingController();

  late final MusicApiService _musicApiService;

  List<MusicSearchItem> _searchResults = [];

  List<MusicItem> _reviewedMusic = [];

  String _selectedFilter = 'ALL';

  bool _isLoading = false;
  bool _hasSearched = false;
  bool _isLoadingReviewedMusic = true;

  String? _errorMessage;

  List<MusicSearchItem> get _filteredSearchResults {
    if (_selectedFilter == 'ALL') {
      return _searchResults;
    }

    return _searchResults
        .where((item) => item.contentType == _selectedFilter)
        .toList();
  }

  @override
  void initState() {
    super.initState();
    _musicApiService = widget.musicApi ?? MusicApiService();
    _loadReviewedMusic();
  }

  Future<void> _loadReviewedMusic() async {
    try {
      final reviewed = await _musicApiService.getReviewedMusic(
        userId: widget.userId,
      );

      if (!mounted) return;

      setState(() {
        _reviewedMusic = reviewed;
        _isLoadingReviewedMusic = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _reviewedMusic = [];
        _isLoadingReviewedMusic = false;
      });
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    if (widget.musicApi == null) _musicApiService.close();
    super.dispose();
  }

  Future<void> _searchMusic() async {
    final query = _searchController.text.trim();

    if (query.isEmpty) {
      setState(() {
        _hasSearched = false;
        _searchResults = [];
        _errorMessage = null;
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _hasSearched = true;
      _errorMessage = null;
    });

    try {
      final results = await _musicApiService.searchMusic(query);

      if (!mounted) return;

      setState(() {
        _searchResults = results;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _searchResults = [];
        _errorMessage = 'No se pudo realizar la búsqueda';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _selectFilter(String filter) {
    setState(() {
      _selectedFilter = filter;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        color: AppColors.cream,
        child: SingleChildScrollView(
          padding: EdgeInsets.symmetric(
            horizontal: MediaQuery.sizeOf(context).width < 800 ? 20 : 48,
            vertical: 36,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Música', style: AppTextStyles.pageTitle),

              const SizedBox(height: 24),

              TextField(
                controller: _searchController,
                textInputAction: TextInputAction.search,
                onSubmitted: (_) => _searchMusic(),
                decoration: InputDecoration(
                  hintText: 'Buscar canciones, álbumes o artistas...',
                  prefixIcon: const Icon(Icons.search, color: AppColors.ink),
                  suffixIcon: IconButton(
                    onPressed: _searchMusic,
                    icon: const Icon(Icons.arrow_forward, color: AppColors.ink),
                  ),
                ),
              ),

              const SizedBox(height: 18),

              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  MusicFilterChip(
                    label: 'Todo',
                    selected: _selectedFilter == 'ALL',
                    onTap: () {
                      _selectFilter('ALL');
                    },
                  ),
                  MusicFilterChip(
                    label: 'Artistas',
                    selected: _selectedFilter == 'ARTIST',
                    onTap: () {
                      _selectFilter('ARTIST');
                    },
                  ),
                  MusicFilterChip(
                    label: 'Álbumes',
                    selected: _selectedFilter == 'ALBUM',
                    onTap: () {
                      _selectFilter('ALBUM');
                    },
                  ),
                  MusicFilterChip(
                    label: 'Canciones',
                    selected: _selectedFilter == 'SONG',
                    onTap: () {
                      _selectFilter('SONG');
                    },
                  ),
                ],
              ),

              const SizedBox(height: 30),

              if (_hasSearched) _buildSearchSection(),

              if (_hasSearched) const SizedBox(height: 38),

              for (final kind in MusicRecommendationKind.values)
                Visibility(
                  visible:
                      !_hasSearched &&
                      (_selectedFilter == 'ALL' ||
                          _selectedFilter == kind.contentType),
                  maintainState: true,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 36),
                    child: HomeMusicRecommendationsSection(
                      key: ValueKey(
                        'music-catalog-${kind.name}-${widget.userId}',
                      ),
                      userId: widget.userId,
                      kind: kind,
                      allowRefresh: false,
                      api: widget.recommendationsApi,
                      onOpenLists: () {},
                    ),
                  ),
                ),

              Wrap(
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 16,
                runSpacing: 8,
                children: [
                  Text(
                    'Tus reseñas musicales',
                    style: AppTextStyles.sectionTitle,
                  ),
                ],
              ),

              const SizedBox(height: 16),

              if (_isLoadingReviewedMusic)
                const Center(
                  child: CircularProgressIndicator(color: AppColors.lavender),
                )
              else if (_reviewedMusic.isEmpty)
                Text(
                  'Todavía no has reseñado música',
                  style: AppTextStyles.secondary,
                )
              else
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: _reviewedMusic
                        .where(
                          (item) =>
                              _selectedFilter == 'ALL' ||
                              item.type.name.toUpperCase() == _selectedFilter,
                        )
                        .map(
                          (item) => Padding(
                            padding: const EdgeInsets.only(right: 16),
                            child: MusicCard(
                              userId: widget.userId,
                              item: item,
                              onReturn: _loadReviewedMusic,
                            ),
                          ),
                        )
                        .toList(),
                  ),
                ),

              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSearchSection() {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(color: AppColors.lavender),
        ),
      );
    }

    if (_errorMessage != null) {
      return Text(_errorMessage!, style: AppTextStyles.secondary);
    }

    if (_filteredSearchResults.isEmpty) {
      return Text(
        _selectedFilter == 'ALL'
            ? 'No se encontraron resultados'
            : 'No se encontraron resultados para este filtro',
        style: AppTextStyles.secondary,
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _SectionTitle(title: 'Resultados', subtitle: _filterSubtitle()),

        const SizedBox(height: 16),

        LayoutBuilder(
          builder: (context, constraints) {
            final width = constraints.maxWidth;

            final columns = width >= 1000
                ? 4
                : width >= 650
                ? 3
                : width >= 420
                ? 2
                : 1;

            return GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _filteredSearchResults.length,
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
                childAspectRatio: 0.72,
              ),
              itemBuilder: (context, index) {
                return _SearchResultCard(
                  item: _filteredSearchResults[index],
                  userId: widget.userId,
                );
              },
            );
          },
        ),
      ],
    );
  }

  String _filterSubtitle() {
    return switch (_selectedFilter) {
      'ARTIST' => 'Artistas encontrados en Spotify',
      'ALBUM' => 'Álbumes encontrados en Spotify',
      'SONG' => 'Canciones encontradas en Spotify',
      _ => 'Contenido encontrado en Spotify',
    };
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String subtitle;

  const _SectionTitle({required this.title, required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: AppTextStyles.sectionTitle),
        const SizedBox(height: 4),
        Text(subtitle, style: AppTextStyles.secondary),
      ],
    );
  }
}

class _SearchResultCard extends StatelessWidget {
  final String userId;
  final MusicSearchItem item;

  const _SearchResultCard({required this.item, required this.userId});

  @override
  Widget build(BuildContext context) {
    final hasImage = item.imageUrl?.trim().isNotEmpty == true;

    return Card(
      clipBehavior: Clip.antiAlias,
      color: AppColors.warmWhite,
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) {
                return MusicDetailPage(item: item, userId: userId);
              },
            ),
          );
        },
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: AspectRatio(
                  aspectRatio: 1,
                  child: hasImage
                      ? Image.network(
                          item.imageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (_, _, _) {
                            return const _ImagePlaceholder();
                          },
                        )
                      : const _ImagePlaceholder(),
                ),
              ),

              const SizedBox(height: 12),

              Text(
                item.name,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.cardTitle,
              ),

              const SizedBox(height: 4),

              Text(
                item.artistName ?? '',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.secondary,
              ),

              const SizedBox(height: 6),

              Text(_typeText(item.contentType), style: AppTextStyles.secondary),
            ],
          ),
        ),
      ),
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

class _ImagePlaceholder extends StatelessWidget {
  const _ImagePlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.lilac,
      child: const Center(
        child: Icon(Icons.music_note, size: 44, color: AppColors.ink),
      ),
    );
  }
}
