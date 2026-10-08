import 'package:flutter/material.dart';

// Core & Theme
import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_radius.dart';
import '../../../../core/theme/app_spacing.dart';
import '../../../../core/theme/app_text_styles.dart';

// Models & Services
import '../../data/models/login_request_model.dart';
import '../../data/services/auth_api_service.dart';
import '../../data/services/auth_session_manager.dart';

// Widgets
import '../widgets/auth_button.dart';
import '../widgets/auth_text_field.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final AuthApiService _authApiService = AuthApiService();
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _obscurePassword = true;
  bool _rememberMe = false;
  bool _isLoading = false;

  @override
  void dispose() {
    // Liberamos la memoria de los controladores cuando la pantalla se destruye
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    // validar campos del formulario
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      // crear el objeto con los datos del formulario
      final request = LoginRequestModel(
        email: _emailController.text.trim(),
        password: _passwordController.text,
        rememberMe: _rememberMe,
      );

      // consumir el endpoint HTTP
      final session = await _authApiService.login(request);

      await AuthSessionManager.instance.setSession(
        session,
        rememberMe: request.rememberMe,
      );

      if (!mounted) return;

      // navegar a la pantalla principal si el backend responde exitosamente
      Navigator.pushReplacementNamed(context, AppRoutes.home);
    } catch (e) {
      if (!mounted) return;

      // mostrar notificación si hay error
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(e.toString()),
          backgroundColor: Colors.redAccent,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
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
                    // Icono decorativo / Logo
                    const Icon(
                      Icons.auto_awesome_outlined,
                      size: 32,
                      color: AppColors.sage,
                    ),
                    const SizedBox(height: AppSpacing.sm),

                    // Título y Subtítulo
                    Text(
                      'Bienvenida a Musa',
                      style: AppTextStyles.pageTitle.copyWith(fontSize: 28),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      'Inicia sesión para continuar.',
                      style: AppTextStyles.secondary,
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Campo de Correo Electronico
                    AuthTextField(
                      controller: _emailController,
                      hintText: 'Usuario o correo electrónico',
                      prefixIcon: Icons.person_outline,
                      keyboardType: TextInputType.emailAddress,
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'El correo es obligatorio';
                        }
                        if (!value.contains('@')) {
                          return 'Formato de correo inválido';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppSpacing.md),

                    // Campo de Contraseña
                    AuthTextField(
                      controller: _passwordController,
                      hintText: 'Contraseña',
                      prefixIcon: Icons.lock_outline,
                      obscureText: _obscurePassword,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                          color: AppColors.textSecondary,
                          size: 20,
                        ),
                        onPressed: () {
                          setState(() {
                            _obscurePassword = !_obscurePassword;
                          });
                        },
                      ),
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'La contraseña es obligatoria';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppSpacing.sm),

                    // Recordarme y Olvidaste tu contraseña
                    Wrap(
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Row(
                          children: [
                            SizedBox(
                              height: 24,
                              width: 24,
                              child: Checkbox(
                                value: _rememberMe,
                                activeColor: AppColors.lavender,
                                checkColor: AppColors.ink,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                onChanged: (value) {
                                  setState(() {
                                    _rememberMe = value ?? false;
                                  });
                                },
                              ),
                            ),
                            const SizedBox(width: AppSpacing.xs),
                            Text('Recordarme', style: AppTextStyles.secondary),
                          ],
                        ),
                        TextButton(
                          onPressed: () {
                            // ForgotPasswordScreen va aquí
                          },
                          child: Text(
                            '¿Olvidaste tu contraseña?',
                            style: AppTextStyles.secondary.copyWith(
                              color: AppColors.lavender,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: AppSpacing.md),

                    // Botón de Iniciar Sesión
                    AuthButton(
                      text: 'Iniciar sesión',
                      isLoading: _isLoading,
                      onPressed: _handleLogin,
                    ),
                    const SizedBox(height: AppSpacing.md),

                    // Divisor visual
                    Row(
                      children: [
                        const Expanded(child: Divider(color: AppColors.border)),
                        Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                          ),
                          child: Text('o', style: AppTextStyles.secondary),
                        ),
                        const Expanded(child: Divider(color: AppColors.border)),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.md),

                    // Botón Google (Visual / Deshabilitado)
                    OutlinedButton(
                      onPressed: null, // Deshabilitado por ahora
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(double.infinity, 48),
                        side: const BorderSide(color: AppColors.border),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppRadius.large),
                        ),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            'G',
                            style: AppTextStyles.button.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(width: AppSpacing.sm),
                          Text(
                            'Continuar con Google',
                            style: AppTextStyles.button,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Enlace a Registro
                    Wrap(
                      alignment: WrapAlignment.center,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Text(
                          '¿No tienes una cuenta? ',
                          style: AppTextStyles.secondary,
                        ),
                        TextButton(
                          onPressed: () {
                            Navigator.pushNamed(context, AppRoutes.register);
                          },
                          style: TextButton.styleFrom(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            minimumSize: Size.zero,
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                          child: Text(
                            'Regístrate',
                            style: AppTextStyles.secondary.copyWith(
                              color: AppColors.lavender,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
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
