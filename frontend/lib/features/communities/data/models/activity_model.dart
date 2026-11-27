class ActivityModel {
  final String id;
  final String communityId;
  final String createdBy;
  final String title;
  final String? description;
  final String location;
  final DateTime startsAt;
  final DateTime? endsAt;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String status; // PROGRAMADA, CANCELADA, COMPLETADA
  final int participantCount;
  final bool isParticipating;

  const ActivityModel({
    required this.id,
    required this.communityId,
    required this.createdBy,
    required this.title, 
    this.description,
    required this.location,
    required this.startsAt,
    this.endsAt,  
    required this.createdAt,
    required this.updatedAt,
    required this.status,
    required this.participantCount,
    required this.isParticipating,
  });

  //Factory constructor para deserializar el JSON que envía Spring Boot
  factory ActivityModel.fromJson(Map<String, dynamic> json) {
    return ActivityModel(
  
      id: json['id'] as String,
      communityId: json['communityId'] as String,
      createdBy: json['createdBy'] as String,
      title: json['title'] as String? ?? '',
      description:json['description'] as String?,
      location: json['location'] as String? ?? '',

      //Fechas (OffsetDateTime en Java llega en formato String ISO-8601)
      startsAt: DateTime.parse(json['startsAt'] as String).toUtc(),


      endsAt: json['endsAt'] != null
          ? DateTime.parse(json['endsAt'] as String).toUtc()
          : null,


      createdAt: DateTime.parse(json['createdAt'] as String).toUtc(),
      updatedAt: DateTime.parse(json['updatedAt'] as String).toUtc(),

      status: json['status'] as String? ?? 'PROGRAMADA',      
      participantCount: (json['participantCount'] as num?)?.toInt() ?? 0,
      isParticipating: json['isParticipating'] as bool? ?? false,
    );
  }

  /// Método para serializar de objeto Dart a JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'communityId': communityId,
      'title': title,
      'description': description,
      'location': location,
      'startsAt': startsAt.toUtc().toIso8601String(),
      'endsAt': endsAt?.toUtc().toIso8601String(),
      'createdAt': createdAt.toUtc().toIso8601String(),
      'updatedAt': updatedAt.toUtc().toIso8601String(),
      'status': status,
      'participantCount': participantCount,
      'isParticipating': isParticipating,
    };
  }
}

class ActivityCreateRequestModel {
  final String title;
  final String? description;
  final String location;
  final DateTime startsAt;
  final DateTime? endsAt;

  const ActivityCreateRequestModel({
    required this.title,
    this.description,
    required this.location,
    required this.startsAt,
    this.endsAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'description': description,
      'location': location,
      'startsAt': startsAt.toUtc().toIso8601String(),
      if (endsAt != null) 'endsAt': endsAt!.toUtc().toIso8601String(),
    };
  }
}

class ActivityUpdateRequestModel {
  final String? title;
  final String? description;
  final String? location;
  final DateTime? startsAt;
  final DateTime? endsAt;
  final String? status;

  const ActivityUpdateRequestModel({
    this.title,
    this.description,
    this.location,
    this.startsAt,
    this.endsAt,
    this.status,
  });

  Map<String, dynamic> toJson() {
    return {
      if (title != null) 'title': title,
      if (description != null) 'description': description,
      if (location != null) 'location': location,
      if (startsAt != null) 'startsAt': startsAt!.toUtc().toIso8601String(),
      if (endsAt != null) 'endsAt': endsAt!.toUtc().toIso8601String(),
      if (status != null) 'status': status,
    };
  }
}
