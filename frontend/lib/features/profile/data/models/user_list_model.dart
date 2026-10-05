class UserListModel {
  final String id;
  final String name;
  final String? description;
  final DateTime createdAt;

  const UserListModel({
    required this.id,
    required this.name,
    required this.description,
    required this.createdAt,
  });

  factory UserListModel.fromJson(Map<String, dynamic> json) {
    return UserListModel(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}