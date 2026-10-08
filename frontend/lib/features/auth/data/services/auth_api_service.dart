import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/auth_response_model.dart';
import '../models/forgot_password_request_model.dart';
import '../models/login_request_model.dart';
import '../models/message_response_model.dart';
import '../models/register_request_model.dart';
import '../models/reset_password_request_model.dart';
import '../models/token_request_model.dart';
import '../models/verify_token_request_model.dart';

class AuthApiException implements Exception {
  final String message;
  final int? statusCode;

  AuthApiException(this.message, {this.statusCode});

  @override
  String toString() => message;
}

class AuthApiService {
  // URL base del backend Spring Boot
  final String baseUrl;
  final http.Client _client;

  AuthApiService({
    this.baseUrl = const String.fromEnvironment(
      'AUTH_API_URL',
      defaultValue: 'http://localhost:8083/api/v1/auth',
    ),
    http.Client? client,
  }) : _client = client ?? http.Client();

  Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  Map<String, dynamic> _readResponse(http.Response response) {
    Map<String, dynamic>? data;
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map<String, dynamic>) data = decoded;
    } on FormatException {
      // Empty or HTML error bodies must keep their real HTTP status.
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = data?['message'];
      final validationMessages = data?.values.whereType<String>().join(' ');
      throw AuthApiException(
        message is String && message.isNotEmpty
            ? message
            : validationMessages != null && validationMessages.isNotEmpty
            ? validationMessages
            : 'El servidor rechazó la solicitud (${response.statusCode}).',
        statusCode: response.statusCode,
      );
    }
    if (data == null) {
      throw AuthApiException(
        'El servidor devolvió una respuesta inválida.',
        statusCode: response.statusCode,
      );
    }
    return data;
  }

  /// Endpoint: POST /api/v1/auth/login
  Future<AuthResponseModel> login(LoginRequestModel request) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/login'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      final data = _readResponse(response);

      if (response.statusCode == 200) {
        return AuthResponseModel.fromJson(data);
      } else {
        final errorMessage = data['message'] ?? 'Error al iniciar sesión';
        throw AuthApiException(errorMessage, statusCode: response.statusCode);
      }
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo conectar con el servidor: ${e.toString()}',
      );
    }
  }

  /// Endpoint: POST /api/v1/auth/register
  Future<MessageResponseModel> register(RegisterRequestModel request) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/register'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      final data = _readResponse(response);

      if (response.statusCode == 200 || response.statusCode == 201) {
        return MessageResponseModel.fromJson(data);
      } else {
        final errorMessage = data['message'] ?? 'Error en el registro';
        throw AuthApiException(errorMessage, statusCode: response.statusCode);
      }
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo conectar con el servidor: ${e.toString()}',
      );
    }
  }

  /// Endpoint: POST /api/v1/auth/forgot-password
  Future<MessageResponseModel> forgotPassword(
    ForgotPasswordRequestModel request,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/forgot-password'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      final data = _readResponse(response);

      if (response.statusCode == 200) {
        return MessageResponseModel.fromJson(data);
      } else {
        final errorMessage =
            data['message'] ??
            'Error al solicitar la recuperación de contraseña';
        throw AuthApiException(errorMessage, statusCode: response.statusCode);
      }
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo conectar con el servidor: ${e.toString()}',
      );
    }
  }

  /// Checks the recovery code from email; this is not an access/refresh token.
  Future<MessageResponseModel> verifyToken(
    VerifyTokenRequestModel request,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/verify-token'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));
      return MessageResponseModel.fromJson(_readResponse(response));
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo verificar el código. Comprueba tu conexión.',
      );
    }
  }

  /// Endpoint: POST /api/v1/auth/reset-password
  Future<MessageResponseModel> resetPassword(
    ResetPasswordRequestModel request,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/reset-password'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      final data = _readResponse(response);

      if (response.statusCode == 200) {
        return MessageResponseModel.fromJson(data);
      } else {
        final errorMessage =
            data['message'] ?? 'Error al restablecer la contraseña';
        throw AuthApiException(errorMessage, statusCode: response.statusCode);
      }
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo conectar con el servidor: ${e.toString()}',
      );
    }
  }

  /// Endpoint: POST /api/v1/auth/refresh-token
  Future<AuthResponseModel> refreshToken(TokenRequestModel request) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/refresh-token'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      final data = _readResponse(response);

      if (response.statusCode == 200) {
        return AuthResponseModel.fromJson(data);
      } else {
        final errorMessage = data['message'] ?? 'Sesión expirada';
        throw AuthApiException(errorMessage, statusCode: response.statusCode);
      }
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException(
        'No se pudo renovar la sesión. Comprueba tu conexión.',
      );
    }
  }

  Future<void> logout(TokenRequestModel request) async {
    try {
      final response = await _client
          .post(
            Uri.parse('$baseUrl/logout'),
            headers: _headers,
            body: jsonEncode(request.toJson()),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        return;
      }

      final data = _readResponse(response);

      throw AuthApiException(
        data['message'] as String? ?? 'No se pudo cerrar la sesión',
        statusCode: response.statusCode,
      );
    } catch (e) {
      if (e is AuthApiException) rethrow;
      throw AuthApiException('No se pudo conectar para cerrar la sesión.');
    }
  }
}
