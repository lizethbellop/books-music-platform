class PreferenceElementModel {
  final String id;
  final String elementType;
  final String referenceId;

  const PreferenceElementModel({
    required this.id,
    required this.elementType,
    required this.referenceId,
  });

  factory PreferenceElementModel.fromJson(Map<String, dynamic> json) {
    return PreferenceElementModel(
      id: json['id'] as String,
      elementType: json['elementType'] as String,
      referenceId: json['referenceId'] as String,
    );
  }
}