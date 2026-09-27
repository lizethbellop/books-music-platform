import 'preference_element_model.dart';

class PreferencesModel {
  final List<PreferenceElementModel> elements;

  const PreferencesModel({
    required this.elements,
  });

  factory PreferencesModel.fromJson(Map<String, dynamic> json) {
    final items = json['elements'] as List<dynamic>;

    return PreferencesModel(
      elements: items
          .map(
            (item) => PreferenceElementModel.fromJson(
              item as Map<String, dynamic>,
            ),
          )
          .toList(),
    );
  }
}