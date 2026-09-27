class AddPreferenceElementRequest {
  final String elementType;
  final String referenceId;

  const AddPreferenceElementRequest({
    required this.elementType,
    required this.referenceId,
  });

  Map<String, dynamic> toJson() {
    return {
      'elementType': elementType,
      'referenceId': referenceId,
    };
  }
}