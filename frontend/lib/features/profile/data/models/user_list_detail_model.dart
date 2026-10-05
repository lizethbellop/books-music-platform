import 'list_element_model.dart';

class UserListDetailModel {
  final String id;
  final String name;
  final String? description;
  final DateTime createdAt;
  final List<ListElementModel> elements;

  const UserListDetailModel({
    required this.id,
    required this.name,
    required this.description,
    required this.createdAt,
    required this.elements,
  });

  factory UserListDetailModel.fromJson(Map<String, dynamic> json) {
    final items = json['elements'] as List<dynamic>;

    return UserListDetailModel(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      elements: items
          .map(
            (item) => ListElementModel.fromJson(
              item as Map<String, dynamic>,
            ),
          )
          .toList(),
    );
  }
}