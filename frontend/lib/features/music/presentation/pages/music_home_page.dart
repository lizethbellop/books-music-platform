import 'package:flutter/material.dart';
import 'music_detail_page.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../shared/widgets/musa_sidebar.dart';

import '../../data/music_item.dart';
import '../../data/models/music_search_item.dart';
import '../../data/services/music_api_service.dart';

import '../widgets/explore_card.dart';
import '../widgets/music_card.dart';
import '../widgets/music_filter_chip.dart';

class MusicHomePage extends StatefulWidget {
  const MusicHomePage({super.key});

  @override
  State<MusicHomePage> createState() => _MusicHomePageState();
}

class _MusicHomePageState extends State<MusicHomePage> {
  final TextEditingController _searchController = TextEditingController();

  final MusicApiService _musicApiService = MusicApiService();

  List<MusicSearchItem> _searchResults = [];

  bool _isLoading = false;
  String? _errorMessage;
  bool _hasSearched = false;

  static const reviewedMusic = [
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
      spotifyId: 'mock_zoe_2',
      title: 'He Wont Go',
      artist: 'Adele',
      imageUrl: '',
      type: MusicType.album,
      rating: 3.5,
      reviewDate: '10 ene 2025',
    ),
    MusicItem(
      spotifyId: 'mock_soda_2',
      title: 'Reptilectric',
      artist: 'Zoé',
      imageUrl: '',
      type: MusicType.album,
      rating: 4.5,
      reviewDate: '5 abr 2025',
    ),
  ];

  Future<void> _searchMusic() async {
    final query = _searchController.text.trim();

    if (query.isEmpty) {
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _hasSearched = true;
    });

    try {
      final results = await _musicApiService.searchMusic(query);

      if (!mounted) return;

      setState(() {
        _searchResults = results;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _searchResults = [];
        _errorMessage = 'No se pudo realizar la búsqueda.';
      });
    } finally {
      if (!mounted) return;

      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          const MusaSidebar(),
          Expanded(
            child: Container(
              color: AppColors.cream,
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(
                  horizontal: 48,
                  vertical: 36,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Música',
                      style: AppTextStyles.pageTitle,
                    ),

                    const SizedBox(height: 24),

                    TextField(
                      controller: _searchController,
                      textInputAction: TextInputAction.search,
                      onSubmitted: (_) => _searchMusic(),
                      decoration: InputDecoration(
                        hintText: 'Buscar canciones, álbumes o artistas...',
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
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        MusicFilterChip(
                          label: 'Todo',
                          selected: true,
                        ),
                        MusicFilterChip(label: 'Artistas'),
                        MusicFilterChip(label: 'Álbumes'),
                        MusicFilterChip(label: 'Canciones'),
                        MusicFilterChip(label: 'Playlists'),
                        MusicFilterChip(label: 'Géneros'),
                      ],
                    ),

                    const SizedBox(height: 30),

                    if (_hasSearched) _buildSearchSection(),

                    if (_hasSearched) const SizedBox(height: 38),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Mis músicas reseñadas',
                          style: AppTextStyles.sectionTitle,
                        ),
                        TextButton(
                          onPressed: () {},
                          child: Text(
                            'Ver más  →',
                            style: AppTextStyles.button.copyWith(
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 16),

                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: reviewedMusic
                            .map(
                              (item) => Padding(
                                padding: const EdgeInsets.only(right: 16),
                                child: MusicCard(item: item),
                              ),
                            )
                            .toList(),
                      ),
                    ),

                    const SizedBox(height: 44),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Explora para ti',
                              style: AppTextStyles.sectionTitle,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Recomendaciones basadas en tus gustos.',
                              style: AppTextStyles.secondary,
                            ),
                          ],
                        ),
                        TextButton(
                          onPressed: () {},
                          child: Text(
                            'Ver más  →',
                            style: AppTextStyles.button.copyWith(
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 16),

                    LayoutBuilder(
                      builder: (context, constraints) {
                        final availableWidth = constraints.maxWidth;
                        final cardWidth = (availableWidth - 48) / 4;

                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            SizedBox(
                              width: cardWidth,
                              child: const ExploreCard(
                                title: 'Indie para tu día',
                                subtitle:
                                    'Artistas y álbumes que te pueden gustar.',
                                color: AppColors.lilac,
                                icon: Icons.album_outlined,
                              ),
                            ),
                            const SizedBox(width: 16),
                            SizedBox(
                              width: cardWidth,
                              child: const ExploreCard(
                                title: 'Clásicos',
                                subtitle:
                                    'Álbumes que todos deberían escuchar.',
                                color: AppColors.mint,
                                icon: Icons.library_music_outlined,
                              ),
                            ),
                            const SizedBox(width: 16),
                            SizedBox(
                              width: cardWidth,
                              child: const ExploreCard(
                                title: 'Nuevos lanzamientos',
                                subtitle:
                                    'Lo más reciente en la escena musical.',
                                color: AppColors.butter,
                                icon: Icons.headphones_outlined,
                              ),
                            ),
                            const SizedBox(width: 16),
                            SizedBox(
                              width: cardWidth,
                              child: const ExploreCard(
                                title: 'Hecho para ti',
                                subtitle:
                                    'Una selección basada en tu actividad.',
                                color: AppColors.lavender,
                                icon: Icons.auto_awesome_outlined,
                              ),
                            ),
                          ],
                        );
                      },
                    ),

                    const SizedBox(height: 42),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Artistas que podrías seguir',
                              style: AppTextStyles.sectionTitle,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Conecta con personas que comparten tus gustos.',
                              style: AppTextStyles.secondary,
                            ),
                          ],
                        ),
                        TextButton(
                          onPressed: () {},
                          child: Text(
                            'Ver más  →',
                            style: AppTextStyles.button.copyWith(
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 18),

                    const Wrap(
                      spacing: 28,
                      runSpacing: 18,
                      children: [
                        _ArtistSuggestion(
                          name: 'Phoebe Bridgers',
                          followers: '2.1 M seguidores',
                        ),
                        _ArtistSuggestion(
                          name: 'Billie Eilish',
                          followers: '6.3 M seguidores',
                        ),
                        _ArtistSuggestion(
                          name: 'Clairo',
                          followers: '1.8 M seguidores',
                        ),
                        _ArtistSuggestion(
                          name: 'Mac DeMarco',
                          followers: '3.4 M seguidores',
                        ),
                        _ArtistSuggestion(
                          name: 'Lana Del Rey',
                          followers: '7.1 M seguidores',
                        ),
                      ],
                    ),

                    const SizedBox(height: 40),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSearchSection() {
    if (_isLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: CircularProgressIndicator(),
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
        Text(
          'Resultados',
          style: AppTextStyles.sectionTitle,
        ),

        const SizedBox(height: 16),

        Wrap(
          spacing: 16,
          runSpacing: 16,
          children: _searchResults.map((item) {
            return _SearchResultCard(item: item);
          }).toList(),
        ),
      ],
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
    return SizedBox(
      width: 220,
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => MusicDetailPage(
                  item: item,
                ),
              ),
            );
          },
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AspectRatio(
                  aspectRatio: 1,
                  child: item.imageUrl != null &&
                          item.imageUrl!.isNotEmpty
                      ? Image.network(
                          item.imageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) {
                            return _imagePlaceholder();
                          },
                        )
                      : _imagePlaceholder(),
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

                const SizedBox(height: 8),

                Text(
                  _typeText(item.contentType),
                  style: AppTextStyles.secondary.copyWith(
                    fontSize: 11,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _imagePlaceholder() {
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

class _ArtistSuggestion extends StatelessWidget {
  final String name;
  final String followers;

  const _ArtistSuggestion({
    required this.name,
    required this.followers,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 210,
      child: Row(
        children: [
          const CircleAvatar(
            radius: 26,
            backgroundColor: AppColors.sage,
            child: Icon(
              Icons.person,
              color: AppColors.warmWhite,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: AppTextStyles.cardTitle.copyWith(
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  followers,
                  style: AppTextStyles.secondary.copyWith(
                    fontSize: 11,
                  ),
                ),
                const SizedBox(height: 6),
                OutlinedButton(
                  onPressed: () {},
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 30),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                    ),
                    side: const BorderSide(
                      color: AppColors.border,
                    ),
                  ),
                  child: const Text('Seguir'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}