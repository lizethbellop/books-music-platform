import 'package:flutter/material.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../shared/models/musa_destination.dart';
import '../../../../shared/widgets/musa_navigation_shell.dart';
import '../../../../shared/widgets/musa_profile_avatar.dart';
import '../../data/exceptions/profile_api_exception.dart';
import '../../data/models/profile_model.dart';
import '../../data/services/profile_api_service.dart';
import '../widgets/profile_collections.dart';
import 'edit_profile_page.dart';
import '../../../auth/data/services/auth_session_manager.dart';

class ProfilePage extends StatefulWidget {
  final String userId;
  final bool embedded;
  final VoidCallback? onProfileUpdated;

  const ProfilePage({
    super.key,
    required this.userId,
    this.embedded = false,
    this.onProfileUpdated,
  });

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  final ProfileApiService _profileApiService = ProfileApiService();

  late Future<ProfileModel> _profileFuture;
  Key _navigationShellKey = UniqueKey();
  bool _loggingOut = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  void _loadProfile() {
    _profileFuture = _profileApiService.getOwnProfile(userId: widget.userId);
  }

  void _retry() {
    setState(() {
      _loadProfile();
      _navigationShellKey = UniqueKey();
    });
  }

  Future<void> _openEditProfile(ProfileModel profile) async {
    final wasUpdated = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (context) {
          return EditProfilePage(userId: widget.userId, profile: profile);
        },
      ),
    );

    if (wasUpdated == true && mounted) {
      _retry();
      widget.onProfileUpdated?.call();
    }
  }

  Future<void> _logout() async {
    if (_loggingOut) return;
    setState(() => _loggingOut = true);
    try {
      await AuthSessionManager.instance.logout();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('No se pudo completar el cierre de sesión.'),
        ),
      );
    } finally {
      if (mounted) setState(() => _loggingOut = false);
    }
  }

  void _selectDestination(MusaDestination destination) {
    switch (destination) {
      case MusaDestination.profile:
        return;

      case MusaDestination.home:
        Navigator.of(context).pushReplacementNamed(AppRoutes.home);
        return;

      case MusaDestination.music:
        Navigator.of(context).pushReplacementNamed(AppRoutes.music);
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
    final profileContent = SafeArea(
      child: FutureBuilder<ProfileModel>(
        future: _profileFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(color: AppColors.lavender),
            );
          }

          if (snapshot.hasError) {
            return _ProfileErrorView(error: snapshot.error!, onRetry: _retry);
          }

          final profile = snapshot.data!;

          return _ProfileContent(
            profile: profile,
            userId: widget.userId,
            onEdit: () => _openEditProfile(profile),
            onLogout: _logout,
            loggingOut: _loggingOut,
          );
        },
      ),
    );

    if (widget.embedded) {
      return ColoredBox(color: AppColors.cream, child: profileContent);
    }

    return MusaNavigationShell(
      key: _navigationShellKey,
      userId: widget.userId,
      selectedDestination: MusaDestination.profile,
      onDestinationSelected: _selectDestination,
      child: profileContent,
    );
  }
}

class _ProfileContent extends StatelessWidget {
  final ProfileModel profile;
  final String userId;
  final VoidCallback onEdit;
  final VoidCallback onLogout;
  final bool loggingOut;

  const _ProfileContent({
    required this.profile,
    required this.userId,
    required this.onEdit,
    required this.onLogout,
    required this.loggingOut,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop = constraints.maxWidth >= 800;

        return SingleChildScrollView(
          padding: EdgeInsets.symmetric(
            horizontal: isDesktop ? 48 : 20,
            vertical: 24,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 980),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (!isDesktop) ...[
                    const _MusaWordmark(width: 115),
                    const SizedBox(height: 32),
                  ],
                  _ProfileHeader(
                    profile: profile,
                    isDesktop: isDesktop,
                    onEdit: onEdit,
                  ),
                  const SizedBox(height: 12),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton.icon(
                      onPressed: loggingOut ? null : onLogout,
                      icon: const Icon(Icons.logout),
                      label: Text(
                        loggingOut ? 'Cerrando sesión...' : 'Cerrar sesión',
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ProfileCollections(userId: userId),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  final ProfileModel profile;
  final bool isDesktop;
  final VoidCallback onEdit;

  const _ProfileHeader({
    required this.profile,
    required this.isDesktop,
    required this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    final information = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          AuthSessionManager.instance.session?.username.isNotEmpty == true
              ? AuthSessionManager.instance.session!.username
              : 'Mi perfil',
          style: AppTextStyles.pageTitle.copyWith(
            fontSize: isDesktop ? 44 : 32,
          ),
        ),
        const SizedBox(height: 12),
        Text(
          profile.biography?.isNotEmpty == true
              ? profile.biography!
              : 'Todavía no has agregado una biografía.',
          style: AppTextStyles.body,
        ),
        const SizedBox(height: 14),
        _PrivacyChip(isPrivate: profile.privateProfile),
      ],
    );

    final editButton = OutlinedButton.icon(
      onPressed: onEdit,
      icon: const Icon(Icons.edit_outlined),
      label: const Text('Editar perfil'),
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.ink,
        side: const BorderSide(color: AppColors.ink),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      ),
    );

    if (isDesktop) {
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _ProfileAvatar(imageUrl: profile.profilePictureUrl, radius: 72),
          const SizedBox(width: 32),
          Expanded(child: information),
          const SizedBox(width: 24),
          editButton,
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _ProfileAvatar(imageUrl: profile.profilePictureUrl, radius: 52),
            const SizedBox(width: 20),
            Expanded(child: information),
          ],
        ),
        const SizedBox(height: 24),
        editButton,
      ],
    );
  }
}

class _ProfileAvatar extends StatelessWidget {
  final String? imageUrl;
  final double radius;

  const _ProfileAvatar({required this.imageUrl, required this.radius});

  @override
  Widget build(BuildContext context) {
    return MusaProfileAvatar(imageUrl: imageUrl, radius: radius);
  }
}

class _PrivacyChip extends StatelessWidget {
  final bool isPrivate;

  const _PrivacyChip({required this.isPrivate});

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(
        isPrivate ? Icons.lock_outline : Icons.public_outlined,
        size: 18,
        color: AppColors.ink,
      ),
      label: Text(isPrivate ? 'Perfil privado' : 'Perfil público'),
      backgroundColor: AppColors.mint,
      side: BorderSide.none,
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

class _ProfileErrorView extends StatelessWidget {
  final Object error;
  final VoidCallback onRetry;

  const _ProfileErrorView({required this.error, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final message = error is ProfileApiException
        ? (error as ProfileApiException).message
        : 'No fue posible conectarse con el servicio de Perfil.';

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: AppColors.ink),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: AppTextStyles.body,
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: onRetry,
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.lavender,
                foregroundColor: AppColors.ink,
              ),
              child: const Text('Volver a intentar'),
            ),
          ],
        ),
      ),
    );
  }
}
