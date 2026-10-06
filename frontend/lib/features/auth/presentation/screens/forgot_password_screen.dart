import 'package:flutter/material.dart';

// Core & Theme
import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_radius.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../core/widgets/app_notification.dart';

// Models & Services
import '../../data/models/forgot_password_request_model.dart';
import '../../data/services/auth_api_service.dart';

// Widgets
import '../widgets/auth_button.dart';
import '../widgets/auth_text_field.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _emailController = TextEditingController();
  final AuthApiService _authApiService = AuthApiService();

  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _handleForgotPassword() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final request = ForgotPasswordRequestModel(
        email: _emailController.text.trim(),
      );

      final response = await _authApiService.forgotPassword(request);

      if (!mounted) return;

      AppNotification.showSuccess(context, response.message);

      // Redirigir a la pantalla para ingresar el token recibido por correo
      Navigator.pushNamed(
        context,
        AppRoutes.verifyToken,
      );
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
                    // Botón de regresar al login
                    Align(
                      alignment: Alignment.centerLeft,
                      child: TextButton.icon(
                        onPressed: () => Navigator.pushReplacementNamed(context, AppRoutes.login),
                        icon: const Icon(Icons.arrow_back, size: 18, color: AppColors.lavender),
                        label: Text(
                          'Volver al inicio de sesión',
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

                    // Icono decorativo / Logo
                    const Icon(
                      Icons.auto_awesome_outlined,
                      size: 32,
                      color: AppColors.sage,
                    ),
                    const SizedBox(height: AppSpacing.sm),

                    // Título y Subtítulo
                    Text(
                      'Recuperar contraseña',
                      style: AppTextStyles.pageTitle.copyWith(fontSize: 26),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      'Ingresa tu correo electrónico asociado a tu cuenta y te enviaremos un token para restablecerla.',
                      style: AppTextStyles.secondary,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Campo de Correo Electrónico
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: AppSpacing.xs),
                        child: Text(
                          'Correo electrónico *',
                          style: AppTextStyles.secondary.copyWith(
                            fontWeight: FontWeight.w600,
                            color: AppColors.ink,
                          ),
                        ),
                      ),
                    ),
                    AuthTextField(
                      controller: _emailController,
                      hintText: 'tu@correo.com',
                      prefixIcon: Icons.mail_outline,
                      keyboardType: TextInputType.emailAddress,
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'El correo es obligatorio';
                        }
                        final emailRegex = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
                        if (!emailRegex.hasMatch(value.trim())) {
                          return 'Ingresa un correo electrónico válido';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Botón de Enviar
                    AuthButton(
                      text: 'Enviar token',
                      isLoading: _isLoading,
                      onPressed: _handleForgotPassword,
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