class UpdateProfileRequest{
    final String? biography;
    final bool privateProfile;

    const UpdateProfileRequest({
        this.biography,
        required this.privateProfile,
    });

    Map<String, dynamic> toJson(){
        return {
            'biography': biography,
            'privateProfile': privateProfile,
        };
    }
}