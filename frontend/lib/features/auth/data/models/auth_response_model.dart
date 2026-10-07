class AuthResponseModel {
  final String accessToken;
  final String refreshToken;
  final String userId;
  final String fullName;
  final String roleName;

  const AuthResponseModel({
    required this.accessToken,
    required this.refreshToken,
    required this.userId,
    required this.fullName,
    required this.roleName,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    return AuthResponseModel(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      userId: json['userId'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
      roleName: json['roleName'] as String? ?? '',
    );
  }
}