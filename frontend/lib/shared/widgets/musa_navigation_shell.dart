import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../features/profile/data/models/profile_model.dart';
import '../../features/profile/data/services/profile_api_service.dart';
import '../models/musa_destination.dart';
import 'musa_bottom_navigation.dart';
import 'musa_sidebar.dart';

class MusaNavigationShell extends StatefulWidget {
  final MusaDestination selectedDestination;
  final ValueChanged<MusaDestination> onDestinationSelected;
  final Widget child;
  final String userId;

  const MusaNavigationShell({
    super.key,
    required this.selectedDestination,
    required this.onDestinationSelected,
    required this.child,
    required this.userId,
  });

  @override
  State<MusaNavigationShell> createState() => _MusaNavigationShellState();
}

class _MusaNavigationShellState extends State<MusaNavigationShell> {
  final ProfileApiService _profileApiService = ProfileApiService();
  late Future<ProfileModel?> _profileFuture;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  void didUpdateWidget(covariant MusaNavigationShell oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.userId != widget.userId) {
      _loadProfile();
    }
  }

  void _loadProfile() {
    _profileFuture = _fetchProfile();
  }

  Future<ProfileModel?> _fetchProfile() async {
    try {
      return await _profileApiService.getOwnProfile(userId: widget.userId);
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop = constraints.maxWidth >= 800;

        return FutureBuilder<ProfileModel?>(
          future: _profileFuture,
          builder: (context, snapshot) {
            final profilePictureUrl = snapshot.data?.profilePictureUrl;

            return Scaffold(
              backgroundColor: AppColors.cream,
              body: isDesktop
                  ? Row(
                      children: [
                        MusaSidebar(
                          selectedDestination: widget.selectedDestination,
                          onDestinationSelected: widget.onDestinationSelected,
                          profilePictureUrl: profilePictureUrl,
                        ),
                        Expanded(child: widget.child),
                      ],
                    )
                  : widget.child,
              bottomNavigationBar: isDesktop
                  ? null
                  : MusaBottomNavigation(
                      selectedDestination: widget.selectedDestination,
                      onDestinationSelected: widget.onDestinationSelected,
                      profilePictureUrl: profilePictureUrl,
                    ),
            );
          },
        );
      },
    );
  }
}
