import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

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
  Uint8List? _selectedPhoto;
  String? _selectedPhotoName;

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

  Future<void> _pickPhoto() async {
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 1200,
        maxHeight: 1200,
        imageQuality: 85,
      );
      if (picked == null) return;
      final bytes = await picked.readAsBytes();
      final extension = picked.name.split('.').last.toLowerCase();
      if (!['jpg', 'jpeg', 'png', 'webp'].contains(extension) ||
          bytes.length > 5 * 1024 * 1024) {
        if (!mounted) return;
        setState(
          () =>
              _errorMessage = 'Elige una imagen JPG, PNG o WebP de hasta 5 MB.',
        );
        return;
      }
      if (!mounted) return;
      setState(() {
        _selectedPhoto = bytes;
        _selectedPhotoName = picked.name;
        _errorMessage = null;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _errorMessage = 'No se pudo seleccionar la imagen.');
    }
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

      if (_selectedPhoto != null) {
        await _profileApiService.uploadOwnPhoto(
          userId: widget.userId,
          bytes: _selectedPhoto!,
          filename: _selectedPhotoName!,
        );
      }

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
        _errorMessage = 'No fue posible conectarse con el servicio de Perfil.';
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
        title: Text('Editar perfil', style: AppTextStyles.sectionTitle),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 640),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Center(
                    child: Stack(
                      clipBehavior: Clip.none,
                      children: [
                        CircleAvatar(
                          radius: 64,
                          backgroundColor: AppColors.mint,
                          backgroundImage: _selectedPhoto != null
                              ? MemoryImage(_selectedPhoto!)
                              : (widget.profile.profilePictureUrl != null &&
                                            widget
                                                .profile
                                                .profilePictureUrl!
                                                .isNotEmpty
                                        ? NetworkImage(
                                            widget.profile.profilePictureUrl!,
                                          )
                                        : null)
                                    as ImageProvider<Object>?,
                          child:
                              _selectedPhoto == null &&
                                  (widget.profile.profilePictureUrl == null ||
                                      widget.profile.profilePictureUrl!.isEmpty)
                              ? const Icon(
                                  Icons.person_outline,
                                  size: 64,
                                  color: AppColors.ink,
                                )
                              : null,
                        ),
                        Positioned(
                          right: -4,
                          bottom: -4,
                          child: Material(
                            color: AppColors.lavender,
                            shape: const CircleBorder(),
                            child: IconButton(
                              tooltip: 'Cambiar foto de perfil',
                              onPressed: _isSaving ? null : _pickPhoto,
                              icon: const Icon(
                                Icons.edit,
                                color: AppColors.ink,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Toca el lápiz para elegir una foto',
                    textAlign: TextAlign.center,
                    style: AppTextStyles.secondary,
                  ),
                  const SizedBox(height: 32),
                  Text('Biografía', style: AppTextStyles.cardTitle),
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
                      border: Border.all(color: AppColors.border),
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
                      padding: const EdgeInsets.symmetric(vertical: 18),
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
                        : Text('Guardar cambios', style: AppTextStyles.button),
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
