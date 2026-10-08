import 'list_element_model.dart';

class PreferenceElementModel extends ListElementModel {
  const PreferenceElementModel({
    required super.id,
    required super.elementType,
    required super.referenceId,
    super.resolutionStatus,
    super.content,
  });

  factory PreferenceElementModel.fromJson(Map<String, dynamic> json) {
    return PreferenceElementModel(
      id: json['id'] as String,
      elementType: json['elementType'] as String,
      referenceId: json['referenceId'] as String,
      resolutionStatus: json['resolutionStatus'] as String? ?? 'UNAVAILABLE',
      content: json['content'] as Map<String, dynamic>?,
    );
  }
}
