import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

import '../exceptions/profile_api_exception.dart';
import '../models/profile_model.dart';
import '../models/update_profile_request.dart';
import '../models/preferences_model.dart';
import '../models/add_preference_element_request.dart';
import '../models/preference_element_model.dart';
import '../models/user_list_model.dart';
import '../models/user_list_detail_model.dart';
import '../models/create_user_list_request.dart';
import '../models/update_user_list_request.dart';
import '../models/add_list_element_request.dart';
import '../models/list_element_model.dart';

class ProfileApiService {
  static const String baseUrl = String.fromEnvironment(
    'PROFILE_API_URL',
    defaultValue: 'http://localhost:8080/api/profiles',
  );

  Future<ProfileModel> getOwnProfile({required String userId}) async {
    final uri = Uri.parse('$baseUrl/me');

    final response = await http.get(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
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
      headers: {'Content-Type': 'application/json', 'X-User-Id': userId},
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;

    return ProfileModel.fromJson(json);
  }

  Future<ProfileModel> uploadOwnPhoto({
    required String userId,
    required Uint8List bytes,
    required String filename,
  }) async {
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$baseUrl/me/photo'),
    )..headers['X-User-Id'] = userId;

    request.files.add(
      http.MultipartFile.fromBytes(
        'file',
        bytes,
        filename: filename,
        contentType: MediaType(
          'image',
          filename.toLowerCase().endsWith('.png')
              ? 'png'
              : filename.toLowerCase().endsWith('.webp')
              ? 'webp'
              : 'jpeg',
        ),
      ),
    );

    final response = await http.Response.fromStream(await request.send());
    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }
    return ProfileModel.fromJson(
      jsonDecode(response.body) as Map<String, dynamic>,
    );
  }

  Future<PreferencesModel> getOwnPreferences({required String userId}) async {
    final uri = Uri.parse('$baseUrl/me/preferences');

    final response = await http.get(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return PreferencesModel.fromJson(json);
  }

  Future<PreferenceElementModel> addPreferenceElement({
    required String userId,
    required AddPreferenceElementRequest request,
  }) async {
    final uri = Uri.parse('$baseUrl/me/preferences');

    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json', 'X-User-Id': userId},
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 201) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return PreferenceElementModel.fromJson(json);
  }

  Future<List<UserListModel>> getOwnLists({required String userId}) async {
    final uri = Uri.parse('$baseUrl/me/lists');

    final response = await http.get(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final items = jsonDecode(response.body) as List<dynamic>;

    return items
        .map((item) => UserListModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<UserListDetailModel> getListDetail({
    required String userId,
    required String listId,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists/$listId');

    final response = await http.get(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return UserListDetailModel.fromJson(json);
  }

  Future<UserListModel> createList({
    required String userId,
    required CreateUserListRequest request,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists');

    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json', 'X-User-Id': userId},
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 201) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return UserListModel.fromJson(json);
  }

  Future<UserListModel> updateList({
    required String userId,
    required String listId,
    required UpdateUserListRequest request,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists/$listId');

    final response = await http.put(
      uri,
      headers: {'Content-Type': 'application/json', 'X-User-Id': userId},
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 200) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return UserListModel.fromJson(json);
  }

  Future<ListElementModel> addListElement({
    required String userId,
    required String listId,
    required AddListElementRequest request,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists/$listId/elements');

    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json', 'X-User-Id': userId},
      body: jsonEncode(request.toJson()),
    );

    if (response.statusCode != 201) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return ListElementModel.fromJson(json);
  }

  Future<void> removeListElement({
    required String userId,
    required String listId,
    required String elementId,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists/$listId/elements/$elementId');

    final response = await http.delete(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 204) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }
  }

  Future<void> deleteList({
    required String userId,
    required String listId,
  }) async {
    final uri = Uri.parse('$baseUrl/me/lists/$listId');

    final response = await http.delete(uri, headers: {'X-User-Id': userId});

    if (response.statusCode != 204) {
      throw ProfileApiException.fromStatusCode(response.statusCode);
    }
  }
}
