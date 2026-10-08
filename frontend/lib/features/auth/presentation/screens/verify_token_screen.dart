import 'package:flutter/material.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_radius.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../core/widgets/app_notification.dart';

import '../../data/models/verify_token_request_model.dart';
import '../../data/services/auth_api_service.dart';
import '../widgets/auth_button.dart';
import '../widgets/auth_text_field.dart';

class VerifyTokenScreen extends StatefulWidget {
  const VerifyTokenScreen({super.key, this.authApiService});

  final AuthApiService? authApiService;

  @override
  State<VerifyTokenScreen> createState() => _VerifyTokenScreenState();
}

class _VerifyTokenScreenState extends State<VerifyTokenScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _tokenController = TextEditingController();
  late final AuthApiService _authApiService =
      widget.authApiService ?? AuthApiService();

  bool _isLoading = false;

  @override
  void dispose() {
    _tokenController.dispose();
    super.dispose();
  }

  Future<void> _handleVerifyToken() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final token = _tokenController.text.trim();
      final request = VerifyTokenRequestModel(token: token);

      final response = await _authApiService.verifyToken(request);

      if (!mounted) return;

      AppNotification.showSuccess(context, response.message);

      // Avanzar a la pantalla de restablecimiento pasando el token verificado
      Navigator.pushNamed(context, AppRoutes.resetPassword, arguments: token);
    } catch (e) {
      if (!mounted) return;
      AppNotification.showError(context, e.toString());
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.cream,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Container(
              constraints: const BoxConstraints(maxWidth: 420),
              padding: const EdgeInsets.all(AppSpacing.lg),
              decoration: BoxDecoration(
                color: AppColors.warmWhite,
                borderRadius: BorderRadius.circular(AppRadius.medium),
                border: Border.all(color: AppColors.border),
              ),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: TextButton.icon(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(
                          Icons.arrow_back,
                          size: 18,
                          color: AppColors.lavender,
                        ),
                        label: Text(
                          'Atrás',
                          style: AppTextStyles.secondary.copyWith(
                            color: AppColors.lavender,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        style: TextButton.styleFrom(
                          padding: EdgeInsets.zero,
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        ),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    const Icon(
                      Icons.mark_email_read_outlined,
                      size: 36,
                      color: AppColors.sage,
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      'Verificar token',
                      style: AppTextStyles.pageTitle.copyWith(fontSize: 24),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      'Ingresa el código o token de recuperación enviado a tu correo.',
                      style: AppTextStyles.secondary,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: AppSpacing.xs),
                        child: Text(
                          'Token de recuperación *',
                          style: AppTextStyles.secondary.copyWith(
                            fontWeight: FontWeight.w600,
                            color: AppColors.ink,
                          ),
                        ),
                      ),
                    ),
                    AuthTextField(
                      controller: _tokenController,
                      hintText: 'Pega tu token aquí',
                      prefixIcon: Icons.key_outlined,
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'El token es obligatorio';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    AuthButton(
                      text: 'Verificar token',
                      isLoading: _isLoading,
                      onPressed: _handleVerifyToken,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
