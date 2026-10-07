class ResetPasswordRequestModel {
  final String token;
  final String newPassword;
  final String confirmNewPassword;

  const ResetPasswordRequestModel({
    required this.token,
    required this.newPassword,
    required this.confirmNewPassword,
  });

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'newPassword': newPassword,
      'confirmNewPassword': confirmNewPassword,
    };
  }
}