import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../data/models/book_statistics.dart';
import '../../data/models/global_statistics.dart';
import '../../data/models/music_statistics.dart';
import '../../data/models/personal_statistics.dart';
import '../../data/models/profile_statistics.dart';
import '../../data/services/statistics_api_service.dart';

class StatisticsPage extends StatefulWidget {
  const StatisticsPage({super.key});

  @override
  State<StatisticsPage> createState() => _StatisticsPageState();
}

class _StatisticsPageState extends State<StatisticsPage> {
  final StatisticsApiService _statisticsApiService = StatisticsApiService();

  late Future<PersonalStatistics> _personalFuture;
  late Future<GlobalStatistics> _globalFuture;

  @override
  void initState() {
    super.initState();
    _loadStatistics();
  }

  void _loadStatistics() {
    _personalFuture =
        _statisticsApiService.getPersonalStatistics();

    _globalFuture =
        _statisticsApiService.getGlobalStatistics();
  }

  Future<void> _refreshStatistics() async {
    setState(_loadStatistics);

    await Future.wait([
      _personalFuture,
      _globalFuture,
    ]);
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          _buildHeader(),
          const TabBar(
            labelColor: AppColors.ink,
            unselectedLabelColor: AppColors.textSecondary,
            indicatorColor: AppColors.sage,
            tabs: [
              Tab(text: 'Mis estadísticas'),
              Tab(text: 'Globales'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildPersonalTab(),
                _buildGlobalTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(28, 28, 28, 16),
      child: Row(
        children: [
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Estadísticas',
                  style: TextStyle(
                    fontSize: 30,
                    fontWeight: FontWeight.w700,
                    color: AppColors.ink,
                  ),
                ),
                SizedBox(height: 6),
                Text(
                  'Descubre tu actividad en musa.',
                  style: TextStyle(
                    fontSize: 15,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Actualizar',
            onPressed: _refreshStatistics,
            icon: const Icon(
              Icons.refresh_rounded,
              color: AppColors.ink,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPersonalTab() {
    return FutureBuilder<PersonalStatistics>(
      future: _personalFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState ==
            ConnectionState.waiting) {
          return const Center(
            child: CircularProgressIndicator(),
          );
        }

        if (snapshot.hasError) {
          return _ErrorView(
            message:
                'No se pudieron cargar tus estadísticas.',
            onRetry: () {
              setState(_loadStatistics);
            },
          );
        }

        final statistics = snapshot.data!;

        return RefreshIndicator(
          onRefresh: _refreshStatistics,
          child: _StatisticsContent(
            music: statistics.music,
            books: statistics.books,
            profile: statistics.profile,
            global: false,
          ),
        );
      },
    );
  }

  Widget _buildGlobalTab() {
    return FutureBuilder<GlobalStatistics>(
      future: _globalFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState ==
            ConnectionState.waiting) {
          return const Center(
            child: CircularProgressIndicator(),
          );
        }

        if (snapshot.hasError) {
          return _ErrorView(
            message:
                'No se pudieron cargar las estadísticas globales.',
            onRetry: () {
              setState(_loadStatistics);
            },
          );
        }

        final statistics = snapshot.data!;

        return RefreshIndicator(
          onRefresh: _refreshStatistics,
          child: _StatisticsContent(
            music: statistics.music,
            books: statistics.books,
            profile: statistics.profile,
            global: true,
          ),
        );
      },
    );
  }
}

class _StatisticsContent extends StatelessWidget {
  final MusicStatistics music;
  final BookStatistics books;
  final ProfileStatistics profile;
  final bool global;

  const _StatisticsContent({
    required this.music,
    required this.books,
    required this.profile,
    required this.global,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final desktop = constraints.maxWidth >= 900;

        return ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(28),
          children: [
            Text(
              global
                  ? 'Actividad de la comunidad'
                  : 'Tu actividad',
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w700,
                color: AppColors.ink,
              ),
            ),
            const SizedBox(height: 18),

            if (desktop)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(child: _musicSection()),
                  const SizedBox(width: 20),
                  Expanded(child: _booksSection()),
                ],
              )
            else ...[
              _musicSection(),
              const SizedBox(height: 20),
              _booksSection(),
            ],

            const SizedBox(height: 20),
            _profileSection(),
          ],
        );
      },
    );
  }

  Widget _musicSection() {
    return _SectionCard(
      title: 'Música',
      icon: Icons.headphones_rounded,
      child: Column(
        children: [
          _StatsGrid(
            items: [
              _StatItem(
                label: 'Reseñas',
                value: music.reviews.toString(),
              ),
              _StatItem(
                label: 'Calificaciones',
                value: music.ratings.toString(),
              ),
              _StatItem(
                label: 'Favoritos',
                value: music.favorites.toString(),
              ),
              _StatItem(
                label: 'Promedio',
                value: music.averageRating == null
                    ? 'Sin datos'
                    : music.averageRating!
                        .toStringAsFixed(1),
              ),
            ],
          ),
          const SizedBox(height: 24),
          SizedBox(
            height: 230,
            child: _StatsBarChart(
              labels: const [
                'Reseñas',
                'Ratings',
                'Favoritos',
              ],
              values: [
                music.reviews.toDouble(),
                music.ratings.toDouble(),
                music.favorites.toDouble(),
              ],
              colors: const [
                AppColors.lavender,
                AppColors.sage,
                AppColors.butter,
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _booksSection() {
    return _SectionCard(
      title: 'Libros',
      icon: Icons.menu_book_rounded,
      child: Column(
        children: [
          _StatsGrid(
            items: [
              _StatItem(
                label: 'Reseñas',
                value: books.reviews.toString(),
              ),
              _StatItem(
                label: 'Promedio',
                value: books.averageRating == null
                    ? 'Sin datos'
                    : books.averageRating!
                        .toStringAsFixed(1),
              ),
              _StatItem(
                label: 'Leídos',
                value: books.read.toString(),
              ),
              _StatItem(
                label: 'Leyendo',
                value: books.reading.toString(),
              ),
              _StatItem(
                label: 'Por leer',
                value: books.toRead.toString(),
              ),
            ],
          ),
          const SizedBox(height: 24),
          SizedBox(
            height: 230,
            child: _StatsBarChart(
              labels: const [
                'Leídos',
                'Leyendo',
                'Por leer',
              ],
              values: [
                books.read.toDouble(),
                books.reading.toDouble(),
                books.toRead.toDouble(),
              ],
              colors: const [
                AppColors.sage,
                AppColors.lavender,
                AppColors.butter,
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _profileSection() {
    return _SectionCard(
      title: global ? 'Actividad social' : 'Tu perfil',
      icon: Icons.person_outline_rounded,
      child: _StatsGrid(
        items: [
          _StatItem(
            label: 'Listas',
            value: profile.lists.toString(),
          ),
          _StatItem(
            label: 'Elementos en listas',
            value: profile.listElements.toString(),
          ),
          _StatItem(
            label: 'Preferencias',
            value: profile.preferenceElements.toString(),
          ),
        ],
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Widget child;

  const _SectionCard({
    required this.title,
    required this.icon,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: AppColors.warmWhite,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(
          color: AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                icon,
                color: AppColors.ink,
              ),
              const SizedBox(width: 10),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.w700,
                  color: AppColors.ink,
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          child,
        ],
      ),
    );
  }
}

class _StatsGrid extends StatelessWidget {
  final List<_StatItem> items;

  const _StatsGrid({
    required this.items,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: items
          .map(
            (item) => _MiniStatCard(
              label: item.label,
              value: item.value,
            ),
          )
          .toList(),
    );
  }
}

class _StatItem {
  final String label;
  final String value;

  const _StatItem({
    required this.label,
    required this.value,
  });
}

class _MiniStatCard extends StatelessWidget {
  final String label;
  final String value;

  const _MiniStatCard({
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 145,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cream,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: AppColors.ink,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatsBarChart extends StatelessWidget {
  final List<String> labels;
  final List<double> values;
  final List<Color> colors;

  const _StatsBarChart({
    required this.labels,
    required this.values,
    required this.colors,
  });

  @override
  Widget build(BuildContext context) {
    final highestValue = values.isEmpty
        ? 0.0
        : values.reduce(math.max);

    final maxY = highestValue <= 0
        ? 1.0
        : highestValue * 1.25;

    return BarChart(
      BarChartData(
        minY: 0,
        maxY: maxY,
        alignment: BarChartAlignment.spaceAround,
        borderData: FlBorderData(show: false),
        gridData: const FlGridData(
          show: true,
          drawVerticalLine: false,
        ),
        barTouchData: BarTouchData(
          enabled: true,
        ),
        titlesData: FlTitlesData(
          topTitles: const AxisTitles(
            sideTitles: SideTitles(
              showTitles: false,
            ),
          ),
          rightTitles: const AxisTitles(
            sideTitles: SideTitles(
              showTitles: false,
            ),
          ),
          leftTitles: const AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 34,
            ),
          ),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 36,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();

                if (index < 0 ||
                    index >= labels.length) {
                  return const SizedBox.shrink();
                }

                return Padding(
                  padding:
                      const EdgeInsets.only(top: 8),
                  child: Text(
                    labels[index],
                    style: const TextStyle(
                      fontSize: 11,
                      color:
                          AppColors.textSecondary,
                    ),
                  ),
                );
              },
            ),
          ),
        ),
        barGroups: List.generate(
          values.length,
          (index) => BarChartGroupData(
            x: index,
            barRods: [
              BarChartRodData(
                toY: values[index],
                width: 24,
                color: colors[index],
                borderRadius:
                    BorderRadius.circular(6),
              ),
            ],
          ),
        ),
      ),
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
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.bar_chart_rounded,
              size: 48,
              color: AppColors.textSecondary,
            ),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.ink,
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: onRetry,
              child: const Text('Reintentar'),
            ),
          ],
        ),
      ),
    );
  }
}