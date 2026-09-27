class CreateUserListRequest {
  final String name;
  final String? description;

  const CreateUserListRequest({
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