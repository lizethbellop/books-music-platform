import 'dart:convert';

import 'package:http/http.dart' as http;

import '../exceptions/profile_api_exception.dart';
import '../models/profile_model.dart';
import '../models/update_profile_request.dart';

class ProfileApiService {
  static const String baseUrl = String.fromEnvironment(
    'PROFILE_API_URL',
    defaultValue: 'http://localhost:8080/api/profiles',
  );

  Future<ProfileModel> getOwnProfile({
    required String userId,
  }) async {
    final uri = Uri.parse('$baseUrl/me');

    final response = await http.get(
      uri,
      headers: {
        'X-User-Id': userId,
      },
    );

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(
        response.statusCode,
      );
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;

    return ProfileModel.fromJson(json);
  }

  Future<ProfileModel> updateOwnProfile({
    required String userId,
    required UpdateProfileRequest request,
  }) async {
    final uri = Uri.parse('$baseUrl/me');

    final response = await http.put(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': userId,
      },
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(
        response.statusCode,
      );
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;

    return ProfileModel.fromJson(json);
  }
}