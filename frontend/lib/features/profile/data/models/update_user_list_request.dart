class UpdateUserListRequest {
  final String name;
  final String? description;

  const UpdateUserListRequest({
    required this.name,
    this.description,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'description': description,
    };
  }
}