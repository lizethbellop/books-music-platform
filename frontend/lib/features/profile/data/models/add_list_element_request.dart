class AddListElementRequest {
  final String elementType;
  final String referenceId;

  const AddListElementRequest({
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