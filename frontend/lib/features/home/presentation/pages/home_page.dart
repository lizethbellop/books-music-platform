import 'package:flutter/material.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../shared/models/musa_destination.dart';
import '../../../../shared/widgets/musa_navigation_shell.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  void _selectDestination(
    BuildContext context,
    MusaDestination destination,
  ) {
    switch (destination) {
      case MusaDestination.home:
        return;

      case MusaDestination.music:
        Navigator.of(context).pushReplacementNamed(
          AppRoutes.music,
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
      selectedDestination: MusaDestination.home,
      onDestinationSelected: (destination) {
        _selectDestination(context, destination);
      },
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
                    'Inicio',
                    style:
                        AppTextStyles.pageTitle.copyWith(
                      fontSize: isDesktop ? 44 : 34,
                    ),
                  ),

                  const SizedBox(height: 8),

                  Text(
                    'Descubre lo que está pasando en Musa.',
                    style: AppTextStyles.secondary,
                  ),

                  const SizedBox(height: 36),

                  Center(
                    child: Container(
                      constraints: const BoxConstraints(
                        maxWidth: 560,
                      ),
                      width: double.infinity,
                      padding: EdgeInsets.all(
                        isDesktop ? 40 : 28,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.warmWhite,
                        borderRadius:
                            BorderRadius.circular(24),
                        border: Border.all(
                          color: AppColors.border,
                        ),
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.auto_awesome_outlined,
                            size: 56,
                            color: AppColors.sage,
                          ),
                          const SizedBox(height: 20),
                          Text(
                            'Todavía no hay contenido',
                            style:
                                AppTextStyles.sectionTitle,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'Aquí aparecerán las novedades, '
                            'recomendaciones y actividad de '
                            'las personas que sigues.',
                            style: AppTextStyles.body,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 24),
                          OutlinedButton.icon(
                            onPressed: () {
                              ScaffoldMessenger.of(context)
                                  .showSnackBar(
                                const SnackBar(
                                  content: Text(
                                    'Explorar se conectará próximamente.',
                                  ),
                                ),
                              );
                            },
                            icon: const Icon(
                              Icons.explore_outlined,
                            ),
                            label: const Text(
                              'Explorar Musa',
                            ),
                            style:
                                OutlinedButton.styleFrom(
                              foregroundColor:
                                  AppColors.ink,
                              side: const BorderSide(
                                color:
                                    AppColors.lavender,
                              ),
                              padding:
                                  const EdgeInsets.symmetric(
                                horizontal: 22,
                                vertical: 14,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
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