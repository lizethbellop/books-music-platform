import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/auth_response_model.dart';

class AuthSessionStorage {
  static const _sessionKey = 'musa.auth.session.v1';

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    mOptions: MacOsOptions(
      usesDataProtectionKeychain: false,
    ),
  );

  Future<void> save(AuthResponseModel session) async {
    await _storage.write(
      key: _sessionKey,
      value: jsonEncode(session.toJson()),
    );
  }

  Future<AuthResponseModel?> read() async {
    final value = await _storage.read(key: _sessionKey);

    if (value == null) {
      return null;
    }

    late final AuthResponseModel session;

    try {
      final json = jsonDecode(value) as Map<String, dynamic>;
      session = AuthResponseModel.fromJson(json);
    } on FormatException {
      await clear();
      return null;
    } on TypeError {
      await clear();
      return null;
    }

    if (!session.sessionExpiresAt.isAfter(DateTime.now().toUtc())) {
      await clear();
      return null;
    }

    return session;
  }

  Future<void> clear() async {
    await _storage.delete(key: _sessionKey);
  }
}