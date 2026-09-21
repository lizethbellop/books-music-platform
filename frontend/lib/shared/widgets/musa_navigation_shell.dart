import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../models/musa_destination.dart';
import 'musa_bottom_navigation.dart';
import 'musa_sidebar.dart';

class MusaNavigationShell extends StatelessWidget {
  final MusaDestination selectedDestination;
  final ValueChanged<MusaDestination> onDestinationSelected;
  final Widget child;

  const MusaNavigationShell({
    super.key,
    required this.selectedDestination,
    required this.onDestinationSelected,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop = constraints.maxWidth >= 800;

        return Scaffold(
          backgroundColor: AppColors.cream,
          body: isDesktop
              ? Row(
                  children: [
                    MusaSidebar(
                      selectedDestination:
                          selectedDestination,
                      onDestinationSelected:
                          onDestinationSelected,
                    ),
                    Expanded(child: child),
                  ],
                )
              : child,
          bottomNavigationBar: isDesktop
              ? null
              : MusaBottomNavigation(
                  selectedDestination:
                      selectedDestination,
                  onDestinationSelected:
                      onDestinationSelected,
                ),
        );
      },
    );
  }
}