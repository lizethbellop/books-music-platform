class RegisterRequestModel {
  final String fullName;
  final String email;
  final String password;
  final String confirmPassword;
  final String roleName;

  RegisterRequestModel({
    required this.fullName,
    required this.email,
    required this.password,
    required this.confirmPassword,
    required this.roleName,
  });

  Map<String, dynamic> toJson() {
    return {
      'fullName': fullName,
      'email': email,
      'password': password,
      'confirmPassword': confirmPassword,
      'roleName': roleName,
    };
  }
}