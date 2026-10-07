class TokenRequestModel {
  final String refreshToken;

  const TokenRequestModel({
    required this.refreshToken,
  });

  Map<String, dynamic> toJson() {
    return {
      'refreshToken': refreshToken,
    };
  }
}