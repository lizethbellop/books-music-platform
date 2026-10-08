class RegisterRequestModel {
  final String username;
  final String fullName;
  final String email;
  final String password;
  final String confirmPassword;
  final String roleName;

  RegisterRequestModel({
    required this.username,
    required this.fullName,
    required this.email,
    required this.password,
    required this.confirmPassword,
    required this.roleName,
  });

  Map<String, dynamic> toJson() {
    return {
      'username': username,
      'fullName': fullName,
      'email': email,
      'password': password,
      'confirmPassword': confirmPassword,
      'roleName': roleName,
    };
  }
}
