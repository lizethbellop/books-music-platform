class ParticipantModel {
  final String id;
  final String activityId;
  final String userId;
  final String status; // CONFIRMADA, CANCELADA
  final DateTime? confirmedAt;

  const ParticipantModel({
    required this.id,
    required this.activityId,
    required this.userId,
    required this.status,
    this.confirmedAt,
  });

  //Factory constructor para deserializar el JSON que envía Spring Boot
  factory ParticipantModel.fromJson(Map<String, dynamic> json) {
    return ParticipantModel(
  
      id: json['id'] as String,
      activityId: json['activityId'] as String,
      userId: json['userId'] as String,
      status: json['status'] as String,
      confirmedAt: json['confirmedAt'] != null
          ? DateTime.parse(json['confirmedAt'] as String).toUtc()
          : null,
    );
  }

  /// Método para serializar de objeto Dart a JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'activityId': activityId,
      'userId': userId,
      'status': status,
      'confirmedAt': confirmedAt?.toUtc().toIso8601String(),
    };
  }
}

