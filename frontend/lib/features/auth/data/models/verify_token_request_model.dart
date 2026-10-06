class VerifyTokenRequestModel {
  final String token;

  const VerifyTokenRequestModel({
    required this.token,
  });

  Map<String, dynamic> toJson() {
    return {
      'token': token,
    };
  }
}