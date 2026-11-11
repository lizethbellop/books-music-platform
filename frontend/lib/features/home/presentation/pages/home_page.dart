import 'package:flutter/material.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../shared/models/musa_destination.dart';
import '../../../../shared/widgets/musa_navigation_shell.dart';
import '../widgets/home_book_recommendations_section.dart';
import '../../data/services/recommendations_api_service.dart';

class HomePage extends StatelessWidget {
  final String userId;
  final bool embedded;
  final VoidCallback? onOpenLists;
  final RecommendationsApiService? recommendationsApi;

  const HomePage({
    super.key,
    required this.userId,
    this.embedded = false,
    this.onOpenLists,
    this.recommendationsApi,
  });

  void _selectDestination(BuildContext context, MusaDestination destination) {
    switch (destination) {
      case MusaDestination.home:
        return;

      case MusaDestination.music:
        Navigator.of(context).pushReplacementNamed(AppRoutes.music);
        return;

      case MusaDestination.profile:
        Navigator.of(context).pushReplacementNamed(AppRoutes.profile);
        return;

      case MusaDestination.books:
        Navigator.of(context).pushReplacementNamed(AppRoutes.books);
        return;

      case MusaDestination.explore:
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Esta sección se conectará próximamente.'),
          ),
        );
        return;
    }
  }

  @override
  Widget build(BuildContext context) {
    final content = SafeArea(
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isDesktop = constraints.maxWidth >= 800;

          return SingleChildScrollView(
            padding: EdgeInsets.symmetric(
              horizontal: isDesktop ? 48 : 20,
              vertical: isDesktop ? 36 : 20,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (!isDesktop) ...[
                  const _MusaWordmark(width: 125),
                  const SizedBox(height: 28),
                ],

                Text(
                  'Inicio',
                  style: AppTextStyles.pageTitle.copyWith(
                    fontSize: isDesktop ? 44 : 34,
                  ),
                ),

                const SizedBox(height: 8),

                Text(
                  'Tu próxima lectura empieza con lo que guardas.',
                  style: AppTextStyles.secondary,
                ),

                const SizedBox(height: 36),

                HomeBookRecommendationsSection(
                  key: ValueKey(userId),
                  userId: userId,
                  api: recommendationsApi,
                  onOpenLists:
                      onOpenLists ??
                      () =>
                          _selectDestination(context, MusaDestination.profile),
                ),
              ],
            ),
          );
        },
      ),
    );
    if (embedded) return content;
    return MusaNavigationShell(
      userId: userId,
      selectedDestination: MusaDestination.home,
      onDestinationSelected: (destination) =>
          _selectDestination(context, destination),
      child: content,
    );
  }
}

class _MusaWordmark extends StatelessWidget {
  final double width;

  const _MusaWordmark({required this.width});

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
