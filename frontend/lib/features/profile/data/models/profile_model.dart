class ProfileModel{
    final String id;
    final String userId;
    final String? biography;
    final String? profilePictureUrl;
    final bool privateProfile;
    final DateTime createdAt;
    final DateTime updatedAt;

    ProfileModel({
        required this.id,
        required this.userId,
        this.biography,
        this.profilePictureUrl,
        this.privateProfile = false,
        required this.createdAt,
        required this.updatedAt,
    });

    factory ProfileModel.fromJson(Map<String, dynamic> json){
        return ProfileModel(
            id: json['id'] as String,
            userId: json['userId'] as String,
            biography: json['biography'] as String?,
            profilePictureUrl: json['profilePictureUrl'] as String?,
            privateProfile: json['privateProfile'] as bool,
            createdAt: DateTime.parse(json['createdAt'] as String),
            updatedAt: DateTime.parse(json['updatedAt'] as String),
        );
    }
}