import 'package:flutter/material.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../shared/models/musa_destination.dart';
import '../../../../shared/widgets/musa_navigation_shell.dart';
import '../../data/models/music_search_item.dart';
import '../../data/music_item.dart';
import '../../data/services/music_api_service.dart';
import '../widgets/explore_card.dart';
import '../widgets/music_card.dart';
import '../widgets/music_filter_chip.dart';
import 'music_detail_page.dart';

class MusicHomePage extends StatefulWidget {
  const MusicHomePage({super.key});

  @override
  State<MusicHomePage> createState() =>
      _MusicHomePageState();
}

class _MusicHomePageState
    extends State<MusicHomePage> {
  final TextEditingController _searchController =
      TextEditingController();

  final MusicApiService _musicApiService =
      MusicApiService();

  List<MusicSearchItem> _searchResults = [];

  bool _isLoading = false;
  bool _hasSearched = false;
  String? _errorMessage;

  static const List<MusicItem> reviewedMusic = [
    MusicItem(
      spotifyId: 'mock_cris_mj',
      title: 'Déjame Pensar',
      artist: 'Cris MJ',
      imageUrl: '',
      type: MusicType.album,
      rating: 5,
      reviewDate: '12 ene 2025',
    ),
    MusicItem(
      spotifyId: 'mock_soda_stereo',
      title: '2K16',
      artist: 'Omar Courtz',
      imageUrl: '',
      type: MusicType.album,
      rating: 5,
      reviewDate: '3 mar 2025',
    ),
    MusicItem(
      spotifyId: 'mock_zoe',
      title: 'Corazón Delator',
      artist: 'Soda Stereo',
      imageUrl: '',
      type: MusicType.album,
      rating: 4,
      reviewDate: '28 feb 2025',
    ),
    MusicItem(
      spotifyId: 'mock_adele',
      title: 'He Won’t Go',
      artist: 'Adele',
      imageUrl: '',
      type: MusicType.album,
      rating: 3.5,
      reviewDate: '10 ene 2025',
    ),
    MusicItem(
      spotifyId: 'mock_zoe_2',
      title: 'Reptilectric',
      artist: 'Zoé',
      imageUrl: '',
      type: MusicType.album,
      rating: 4.5,
      reviewDate: '5 abr 2025',
    ),
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _searchMusic() async {
    final query = _searchController.text.trim();

    if (query.isEmpty) {
      return;
    }

    setState(() {
      _isLoading = true;
      _hasSearched = true;
      _errorMessage = null;
    });

    try {
      final results =
          await _musicApiService.searchMusic(query);

      if (!mounted) return;

      setState(() {
        _searchResults = results;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _searchResults = [];
        _errorMessage =
            'No se pudo realizar la búsqueda.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _selectDestination(
    MusaDestination destination,
  ) {
    switch (destination) {
      case MusaDestination.music:
        return;

      case MusaDestination.home:
        Navigator.of(context).pushReplacementNamed(
          AppRoutes.home,
        );
        return;

      case MusaDestination.profile:
        Navigator.of(context).pushReplacementNamed(
          AppRoutes.profile,
        );
        return;

      case MusaDestination.explore:
      case MusaDestination.books:
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Esta sección se conectará próximamente.',
            ),
          ),
        );
        return;
    }
  }

  @override
  Widget build(BuildContext context) {
    return MusaNavigationShell(
      selectedDestination: MusaDestination.music,
      onDestinationSelected: _selectDestination,
      child: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final isDesktop =
                constraints.maxWidth >= 800;

            return SingleChildScrollView(
              padding: EdgeInsets.symmetric(
                horizontal: isDesktop ? 48 : 20,
                vertical: isDesktop ? 36 : 20,
              ),
              child: Column(
                crossAxisAlignment:
                    CrossAxisAlignment.start,
                children: [
                  if (!isDesktop) ...[
                    const _MusaWordmark(
                      width: 125,
                    ),
                    const SizedBox(height: 28),
                  ],

                  Text(
                    'Música',
                    style:
                        AppTextStyles.pageTitle.copyWith(
                      fontSize: isDesktop ? 44 : 36,
                    ),
                  ),

                  const SizedBox(height: 8),

                  Text(
                    'Busca, descubre y guarda la música '
                    'que forma parte de tu historia.',
                    style: AppTextStyles.secondary,
                  ),

                  const SizedBox(height: 24),

                  TextField(
                    controller: _searchController,
                    textInputAction:
                        TextInputAction.search,
                    onSubmitted: (_) => _searchMusic(),
                    decoration: InputDecoration(
                      hintText:
                          'Buscar canciones, álbumes o artistas...',
                      prefixIcon: const Icon(
                        Icons.search,
                        color: AppColors.ink,
                      ),
                      suffixIcon: IconButton(
                        onPressed: _searchMusic,
                        icon: const Icon(
                          Icons.arrow_forward,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                  ),

                  const SizedBox(height: 18),

                  const Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      MusicFilterChip(
                        label: 'Todo',
                        selected: true,
                      ),
                      MusicFilterChip(
                        label: 'Artistas',
                      ),
                      MusicFilterChip(
                        label: 'Álbumes',
                      ),
                      MusicFilterChip(
                        label: 'Canciones',
                      ),
                      MusicFilterChip(
                        label: 'Playlists',
                      ),
                      MusicFilterChip(
                        label: 'Géneros',
                      ),
                    ],
                  ),

                  if (_hasSearched) ...[
                    const SizedBox(height: 32),
                    _buildSearchSection(),
                  ],

                  const SizedBox(height: 40),

                  const _SectionTitle(
                    title: 'Tus reseñas musicales',
                    subtitle:
                        'Álbumes y canciones que ya calificaste.',
                  ),

                  const SizedBox(height: 16),

                  SizedBox(
                    height: 350,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      itemCount:
                          reviewedMusic.length,
                      separatorBuilder: (_, __) {
                        return const SizedBox(
                          width: 16,
                        );
                      },
                      itemBuilder: (context, index) {
                        return MusicCard(
                          item:
                              reviewedMusic[index],
                        );
                      },
                    ),
                  ),

                  const SizedBox(height: 40),

                  const _SectionTitle(
                    title: 'Explora para ti',
                    subtitle:
                        'Recomendaciones basadas en tus gustos.',
                  ),

                  const SizedBox(height: 16),

                  const _ExploreGrid(),

                  const SizedBox(height: 40),

                  const _SectionTitle(
                    title: 'Artistas por descubrir',
                    subtitle:
                        'Nuevos sonidos relacionados con tus gustos.',
                  ),

                  const SizedBox(height: 18),

                  const Wrap(
                    spacing: 18,
                    runSpacing: 18,
                    children: [
                      _ArtistSuggestion(
                        name: 'Phoebe Bridgers',
                      ),
                      _ArtistSuggestion(
                        name: 'Billie Eilish',
                      ),
                      _ArtistSuggestion(
                        name: 'Clairo',
                      ),
                      _ArtistSuggestion(
                        name: 'Mac DeMarco',
                      ),
                    ],
                  ),

                  const SizedBox(height: 40),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildSearchSection() {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(
            color: AppColors.lavender,
          ),
        ),
      );
    }

    if (_errorMessage != null) {
      return Text(
        _errorMessage!,
        style: AppTextStyles.secondary,
      );
    }

    if (_searchResults.isEmpty) {
      return Text(
        'No se encontraron resultados.',
        style: AppTextStyles.secondary,
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionTitle(
          title: 'Resultados',
          subtitle:
              'Contenido encontrado en Spotify.',
        ),

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
              physics:
                  const NeverScrollableScrollPhysics(),
              itemCount: _searchResults.length,
              gridDelegate:
                  SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
                childAspectRatio: 0.72,
              ),
              itemBuilder: (context, index) {
                return _SearchResultCard(
                  item: _searchResults[index],
                );
              },
            );
          },
        ),
      ],
    );
  }
}

class _MusaWordmark extends StatelessWidget {
  final double width;

  const _MusaWordmark({
    required this.width,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      height: 50,
      child: ClipRect(
        child: Image.asset(
          'assets/images/musa_logo.png',
          fit: BoxFit.cover,
          alignment: Alignment.center,
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String subtitle;

  const _SectionTitle({
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment:
          CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: AppTextStyles.sectionTitle,
        ),
        const SizedBox(height: 4),
        Text(
          subtitle,
          style: AppTextStyles.secondary,
        ),
      ],
    );
  }
}

class _ExploreGrid extends StatelessWidget {
  const _ExploreGrid();

  @override
  Widget build(BuildContext context) {
    const cards = [
      ExploreCard(
        title: 'Indie para tu día',
        subtitle:
            'Artistas y álbumes que te pueden gustar.',
        color: AppColors.lilac,
        icon: Icons.album_outlined,
      ),
      ExploreCard(
        title: 'Clásicos',
        subtitle:
            'Álbumes que todos deberían escuchar.',
        color: AppColors.mint,
        icon: Icons.library_music_outlined,
      ),
      ExploreCard(
        title: 'Nuevos lanzamientos',
        subtitle:
            'Lo más reciente de la escena musical.',
        color: AppColors.butter,
        icon: Icons.headphones_outlined,
      ),
      ExploreCard(
        title: 'Hecho para ti',
        subtitle:
            'Una selección basada en tu actividad.',
        color: AppColors.lavender,
        icon: Icons.auto_awesome_outlined,
      ),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;

        final columns = width >= 1100
            ? 4
            : width >= 650
                ? 2
                : 1;

        return GridView.count(
          shrinkWrap: true,
          physics:
              const NeverScrollableScrollPhysics(),
          crossAxisCount: columns,
          crossAxisSpacing: 16,
          mainAxisSpacing: 16,
          childAspectRatio:
              columns == 1 ? 1.8 : 1.45,
          children: cards,
        );
      },
    );
  }
}

class _SearchResultCard extends StatelessWidget {
  final MusicSearchItem item;

  const _SearchResultCard({
    required this.item,
  });

  @override
  Widget build(BuildContext context) {
    final hasImage =
        item.imageUrl?.trim().isNotEmpty == true;

    return Card(
      clipBehavior: Clip.antiAlias,
      color: AppColors.warmWhite,
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) {
                return MusicDetailPage(
                  item: item,
                );
              },
            ),
          );
        },
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment:
                CrossAxisAlignment.start,
            children: [
              Expanded(
                child: AspectRatio(
                  aspectRatio: 1,
                  child: hasImage
                      ? Image.network(
                          item.imageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder:
                              (_, __, ___) {
                            return const
                                _ImagePlaceholder();
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
              Text(
                _typeText(item.contentType),
                style: AppTextStyles.secondary,
              ),
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
        child: Icon(
          Icons.music_note,
          size: 44,
          color: AppColors.ink,
        ),
      ),
    );
  }
}

class _ArtistSuggestion extends StatelessWidget {
  final String name;

  const _ArtistSuggestion({
    required this.name,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 220,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.warmWhite,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: AppColors.border,
        ),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 25,
            backgroundColor: AppColors.sage,
            child: Icon(
              Icons.person_outline,
              color: AppColors.warmWhite,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              name,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: AppTextStyles.cardTitle,
            ),
          ),
        ],
      ),
    );
  }
}