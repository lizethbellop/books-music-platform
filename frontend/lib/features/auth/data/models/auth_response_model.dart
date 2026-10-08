class AuthResponseModel {
  final String accessToken;
  final String refreshToken;
  final String userId;
  final String username;
  final String fullName;
  final String roleName;
  final DateTime accessTokenExpiresAt;
  final DateTime sessionExpiresAt;

  const AuthResponseModel({
    required this.accessToken,
    required this.refreshToken,
    required this.userId,
    required this.username,
    required this.fullName,
    required this.roleName,
    required this.accessTokenExpiresAt,
    required this.sessionExpiresAt,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    return AuthResponseModel(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      userId: json['userId'] as String,
      username: json['username'] as String? ?? '',
      fullName: json['fullName'] as String,
      roleName: json['roleName'] as String,
      accessTokenExpiresAt: json['username'] == null
          ? DateTime.fromMillisecondsSinceEpoch(0, isUtc: true)
          : DateTime.parse(json['accessTokenExpiresAt'] as String).toUtc(),
      sessionExpiresAt: DateTime.parse(json['sessionExpiresAt'] as String)
          .toUtc(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'userId': userId,
      'username': username,
      'fullName': fullName,
      'roleName': roleName,
      'accessTokenExpiresAt': accessTokenExpiresAt.toUtc().toIso8601String(),
      'sessionExpiresAt': sessionExpiresAt.toUtc().toIso8601String(),
    };
  }
}
