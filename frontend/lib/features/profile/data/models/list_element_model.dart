class ListElementModel {
  final String id;
  final String elementType;
  final String referenceId;

  const ListElementModel({
    required this.id,
    required this.elementType,
    required this.referenceId,
  });

  factory ListElementModel.fromJson(Map<String, dynamic> json) {
    return ListElementModel(
      id: json['id'] as String,
      elementType: json['elementType'] as String,
      referenceId: json['referenceId'] as String,
    );
  }
}