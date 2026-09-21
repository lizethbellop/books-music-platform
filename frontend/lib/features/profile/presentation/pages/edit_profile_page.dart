import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/exceptions/profile_api_exception.dart';
import '../../data/models/profile_model.dart';
import '../../data/models/update_profile_request.dart';
import '../../data/services/profile_api_service.dart';

class EditProfilePage extends StatefulWidget {
  final String userId;
  final ProfileModel profile;

  const EditProfilePage({
    super.key,
    required this.userId,
    required this.profile,
  });

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  final ProfileApiService _profileApiService = ProfileApiService();

  late final TextEditingController _biographyController;
  late bool _privateProfile;

  bool _isSaving = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();

    _biographyController = TextEditingController(
      text: widget.profile.biography ?? '',
    );

    _privateProfile = widget.profile.privateProfile;
  }

  @override
  void dispose() {
    _biographyController.dispose();
    super.dispose();
  }

  Future<void> _saveProfile() async {
    setState(() {
      _isSaving = true;
      _errorMessage = null;
    });

    try {
      await _profileApiService.updateOwnProfile(
        userId: widget.userId,
        request: UpdateProfileRequest(
          biography: _biographyController.text.trim(),
          privateProfile: _privateProfile,
        ),
      );

      if (!mounted) return;

      Navigator.of(context).pop(true);
    } on ProfileApiException catch (error) {
      if (!mounted) return;

      setState(() {
        _errorMessage = error.message;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _errorMessage =
            'No fue posible conectarse con el servicio de Perfil.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      appBar: AppBar(
        backgroundColor: AppColors.cream,
        foregroundColor: AppColors.ink,
        title: Text(
          'Editar perfil',
          style: AppTextStyles.sectionTitle,
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: 640,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Biografía',
                    style: AppTextStyles.cardTitle,
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _biographyController,
                    maxLength: 500,
                    maxLines: 5,
                    minLines: 3,
                    decoration: const InputDecoration(
                      hintText:
                          'Cuéntanos sobre tus libros y música favoritos.',
                    ),
                  ),
                  const SizedBox(height: 24),
                  Container(
                    decoration: BoxDecoration(
                      color: AppColors.warmWhite,
                      border: Border.all(
                        color: AppColors.border,
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: SwitchListTile(
                      value: _privateProfile,
                      onChanged: _isSaving
                          ? null
                          : (value) {
                              setState(() {
                                _privateProfile = value;
                              });
                            },
                      activeThumbColor: AppColors.ink,
                      activeTrackColor: AppColors.lavender,
                      title: Text(
                        'Perfil privado',
                        style: AppTextStyles.cardTitle,
                      ),
                      subtitle: Text(
                        _privateProfile
                            ? 'Solo las personas autorizadas podrán ver tu perfil.'
                            : 'Tu perfil será visible para otros usuarios.',
                        style: AppTextStyles.secondary,
                      ),
                    ),
                  ),
                  if (_errorMessage != null) ...[
                    const SizedBox(height: 20),
                    Text(
                      _errorMessage!,
                      style: AppTextStyles.body.copyWith(
                        color: Colors.red.shade700,
                      ),
                    ),
                  ],
                  const SizedBox(height: 32),
                  FilledButton(
                    onPressed: _isSaving ? null : _saveProfile,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.lavender,
                      foregroundColor: AppColors.ink,
                      padding: const EdgeInsets.symmetric(
                        vertical: 18,
                      ),
                    ),
                    child: _isSaving
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: AppColors.ink,
                            ),
                          )
                        : Text(
                            'Guardar cambios',
                            style: AppTextStyles.button,
                          ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}