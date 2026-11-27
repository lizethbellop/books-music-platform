import 'dart:async';
import 'package:flutter/material.dart';

import '../../data/models/community_model.dart';
import '../../data/services/community_api_service.dart';
import '../widgets/community_card.dart';
import '../widgets/create_community_dialog.dart';
import 'community_detail_page.dart';

class ExploreCommunitiesPage extends StatefulWidget {
  final String userId;

  const ExploreCommunitiesPage({
    super.key,
    required this.userId,
  });

  @override
  State<ExploreCommunitiesPage> createState() => _ExploreCommunitiesPageState();
}

class _ExploreCommunitiesPageState extends State<ExploreCommunitiesPage> {
  final CommunityApiService _communityService = CommunityApiService();
  final TextEditingController _searchController = TextEditingController();

  List<CommunityModel> _communities = [];
  bool _isLoading = true;
  String? _errorMessage;
  String _selectedType = 'TODAS';
  Timer? _debounce;
  final Set<String> _joiningIds = {};

  static const _filterOptions = [
    {'value': 'TODAS', 'label': 'Todas'},
    {'value': 'LIBROS', 'label': 'Libros'},
    {'value': 'MUSICA', 'label': 'Música'},
    {'value': 'MIXTO', 'label': 'Mixto'},
  ];

  @override
  void initState() {
    super.initState();
    _loadCommunities();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadCommunities() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final results = await _communityService.searchCommunities(
        query: _searchController.text,
        type: _selectedType,
      );
      if (!mounted) return;
      setState(() {
        _communities = results;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  void _onSearchChanged(String _) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      _loadCommunities();
    });
  }

  void _onTypeSelected(String type) {
    if (_selectedType == type) return;
    setState(() => _selectedType = type);
    _loadCommunities();
  }

  Future<void> _openCreateCommunityDialog() async {
    final request = await showDialog<CommunityCreateRequestModel>(
      context: context,
      builder: (_) => const CreateCommunityDialog(),
    );

    if (request == null || !mounted) return;

    try {
      await _communityService.createCommunity(request);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Comunidad "${request.name}" creada con éxito.')),
      );
      await _loadCommunities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  Future<void> _handleJoinCommunity(CommunityModel community) async {
    setState(() => _joiningIds.add(community.id));

    try {
      await _communityService.joinCommunity(community.id);
      if (!mounted) return;
        final message = community.isPrivate
            ? 'Solicitud enviada a "${community.name}".'
            : 'Te uniste a "${community.name}".';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message)),
        );
      await _loadCommunities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    } finally {
      if (mounted) {
        setState(() => _joiningIds.remove(community.id));
      }
    }
  }

  Future<void> _openCommunityDetail(CommunityModel community) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CommunityDetailPage(
          initialCommunity: community,
          currentUserId: widget.userId,
        ),
      ),
    );
    if (mounted) {
      _loadCommunities();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openCreateCommunityDialog,
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nueva comunidad'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Encabezado
              Text(
                'Comunidades',
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Explora grupos de lectura, música y debate cultural.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 16),

              // Barra de búsqueda
              TextField(
                controller: _searchController,
                onChanged: _onSearchChanged,
                decoration: InputDecoration(
                  hintText: 'Buscar comunidades por nombre...',
                  prefixIcon: const Icon(Icons.search_rounded),
                  suffixIcon: _searchController.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.close_rounded),
                          onPressed: () {
                            _searchController.clear();
                            setState(() {});
                            _loadCommunities();
                          },
                        )
                      : null,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                ),
              ),
              const SizedBox(height: 12),

              // Chips de filtro
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: _filterOptions.map((option) {
                    final value = option['value']!;
                    final label = option['label']!;
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(label),
                        selected: _selectedType == value,
                        onSelected: (_) => _onTypeSelected(value),
                      ),
                    );
                  }).toList(),
                ),
              ),
              const SizedBox(height: 16),

              // Contenido principal
              Expanded(child: _buildBody(theme)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBody(ThemeData theme) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.cloud_off_rounded,
              size: 48,
              color: theme.colorScheme.error,
            ),
            const SizedBox(height: 12),
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyLarge,
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _loadCommunities,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }

    if (_communities.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.groups_outlined,
              size: 52,
              color: theme.colorScheme.onSurfaceVariant,
            ),
            const SizedBox(height: 12),
            Text(
              'No encontramos comunidades',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Prueba con otro filtro o crea la primera comunidad.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadCommunities,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isWide = constraints.maxWidth >= 700;

          if (!isWide) {
            return ListView.separated(
              padding: const EdgeInsets.only(bottom: 88),
              itemCount: _communities.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final community = _communities[index];
                return CommunityCard(
                  community: community,
                  isJoining: _joiningIds.contains(community.id),
                  onTap: () => _openCommunityDetail(community),
                  onJoinTap: () => _handleJoinCommunity(community),
                );
              },
            );
          }

          return GridView.builder(
            padding: const EdgeInsets.only(bottom: 88),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 14,
              mainAxisSpacing: 14,
              mainAxisExtent: 195,
            ),
            itemCount: _communities.length,
            itemBuilder: (context, index) {
              final community = _communities[index];
              return CommunityCard(
                community: community,
                isJoining: _joiningIds.contains(community.id),
                onTap: () => _openCommunityDetail(community),
                onJoinTap: () => _handleJoinCommunity(community),
              );
            },
          );
        },
      ),
    );
  }
}