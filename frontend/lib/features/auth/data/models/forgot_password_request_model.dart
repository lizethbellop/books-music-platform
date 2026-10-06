class ForgotPasswordRequestModel {
  final String email;

  const ForgotPasswordRequestModel({
    required this.email,
  });

  Map<String, dynamic> toJson() {
    return {
      'email': email,
    };
  }
}