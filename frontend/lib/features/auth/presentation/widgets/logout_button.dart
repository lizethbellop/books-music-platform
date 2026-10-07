import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../../core/routes/app_routes.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../data/models/token_request_model.dart';
import '../../data/services/auth_api_service.dart';

class LogoutButton extends StatelessWidget {
  final AuthApiService _authApiService = AuthApiService();
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  LogoutButton({super.key});

  Future<void> _handleLogout(BuildContext context) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cerrar sesión'),
        content: const Text('¿Estás seguro de que deseas salir de Musa?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Salir', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirmar != true || !context.mounted) return;

    try {
      final refreshToken = await _storage.read(key: 'refresh_token');
      if (refreshToken != null && refreshToken.isNotEmpty) {
        await _authApiService.logout(TokenRequestModel(refreshToken: refreshToken));
      }
    } catch (e) {
      debugPrint('Error en logout del backend: $e');
    } finally {
      await _storage.deleteAll();
      if (context.mounted) {
        Navigator.pushNamedAndRemoveUntil(
          context,
          AppRoutes.login,
          (route) => false,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 7),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        child: ListTile(
          dense: true,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          onTap: () => _handleLogout(context),
          leading: const Icon(Icons.logout_rounded, color: Colors.redAccent),
          title: Text(
            'Cerrar sesión',
            style: AppTextStyles.navigation.copyWith(
              color: Colors.redAccent,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}