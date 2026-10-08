import 'package:flutter/foundation.dart';

import '../models/auth_response_model.dart';
import '../models/token_request_model.dart';
import 'auth_api_service.dart';
import 'auth_session_storage.dart';

class AuthSessionManager extends ChangeNotifier {
  AuthSessionManager._();

  static final AuthSessionManager instance = AuthSessionManager._();

  final AuthSessionStorage _storage = AuthSessionStorage();
  final AuthApiService _api = AuthApiService();

  AuthResponseModel? _session;
  bool _rememberMe = false;
  int _sessionVersion = 0;

  Future<String>? _refreshInProgress;
  Future<void> _storageQueue = Future<void>.value();

  AuthResponseModel? get session => _session;
  bool get rememberMe => _rememberMe;

  // Ejecuta las operaciones de almacenamiento en orden.
  Future<T> _withStorage<T>(Future<T> Function() action) {
    final operation = _storageQueue.then((_) => action());

    _storageQueue = operation.then<void>(
      (_) {},
      onError: (Object error, StackTrace stackTrace) {},
    );

    return operation;
  }

  Future<void> setSession(
    AuthResponseModel session, {
    required bool rememberMe,
  }) async {
    _sessionVersion++;
    _session = session;
    _rememberMe = rememberMe;
    notifyListeners();

    await _withStorage<void>(() async {
      if (rememberMe) {
        await _storage.save(session);
      } else {
        await _storage.clear();
      }
    });
  }

  Future<void> restore() async {
    final version = _sessionVersion;

    final savedSession = await _withStorage<AuthResponseModel?>(
      () => _storage.read(),
    );

    // No sobrescribir un login o cierre ocurrido durante la lectura.
    if (version != _sessionVersion) {
      return;
    }

    _sessionVersion++;
    _session = savedSession;
    _rememberMe = savedSession != null;
    notifyListeners();
  }

  Future<void> clearLocalSession() async {
    _sessionVersion++;
    _session = null;
    _rememberMe = false;
    notifyListeners();

    await _withStorage<void>(() => _storage.clear());
  }

  Future<String> getAccessToken() async {
    final current = _session;

    if (current == null) {
      throw AuthApiException(
        'Inicia sesión para continuar.',
        statusCode: 401,
      );
    }

    final now = DateTime.now().toUtc();

    if (!current.sessionExpiresAt.isAfter(now)) {
      await clearLocalSession();

      throw AuthApiException(
        'Tu sesión venció. Inicia sesión nuevamente.',
        statusCode: 401,
      );
    }

    final renewalThreshold = now.add(
      const Duration(seconds: 30),
    );

    if (current.accessTokenExpiresAt.isAfter(renewalThreshold)) {
      return current.accessToken;
    }

    final pending = _refreshInProgress;

    if (pending != null) {
      return pending;
    }

    final refresh = _renewAccessToken(
      current,
      _sessionVersion,
      _rememberMe,
    );

    _refreshInProgress = refresh;

    try {
      return await refresh;
    } finally {
      if (identical(_refreshInProgress, refresh)) {
        _refreshInProgress = null;
      }
    }
  }

  Future<String> _renewAccessToken(
    AuthResponseModel previous,
    int version,
    bool rememberMe,
    ) async {
    late final AuthResponseModel renewed;

    try {
      renewed = await _api.refreshToken(
        TokenRequestModel(
          refreshToken: previous.refreshToken,
        ),
      );
    } on AuthApiException catch (e) {
      final sessionIsUnchanged = version == _sessionVersion;

      if (sessionIsUnchanged &&
          (e.statusCode == 401 || e.statusCode == 403)) {
        await clearLocalSession();
      }

      rethrow;
    }

    if (version != _sessionVersion) {
      await _api.logout(
        TokenRequestModel(
          refreshToken: renewed.refreshToken,
        ),
      );

      throw AuthApiException(
        'La sesión cambió durante la renovación.',
        statusCode: 401,
      );
    }

    if (renewed.userId != previous.userId) {
      await clearLocalSession();

      throw AuthApiException(
        'La respuesta de renovación no corresponde al usuario.',
        statusCode: 401,
      );
    }

    await setSession(
      renewed,
      rememberMe: rememberMe,
    );

    return renewed.accessToken;
  }

  Future<void> logout() async {
    final current = _session;

    try {
      await clearLocalSession();
    } finally {
      if (current != null) {
        await _api.logout(
          TokenRequestModel(
            refreshToken: current.refreshToken,
          ),
        );
      }
    }
  }
}