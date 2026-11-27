class CommunityModel {
  final String id;
  final String name;
  final String? description;
  final String type; // LIBROS, MUSICA, MIXTO
  final String visibility; // PUBLICA, PRIVADA
  final String createdBy;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final String status; // ACTIVA, ELIMINADA
  final int memberCount;
  final bool isMember;
  final String? userRole; // ADMINISTRADOR, MIEMBRO o null
  final String? membershipStatus; // ACTIVO, PENDIENTE o null

  const CommunityModel({
    required this.id,
    required this.name,
    this.description,
    required this.type,
    required this.visibility,
    required this.createdBy,
    this.createdAt,
    this.updatedAt,
    required this.status,
    required this.memberCount,
    required this.isMember,
    this.userRole,
    this.membershipStatus,
  });

  bool get isPrivate => visibility == 'PRIVADA';
  bool get isPending => membershipStatus == 'PENDIENTE';
  bool get isAdmin => isMember && userRole == 'ADMINISTRADOR';

  factory CommunityModel.fromJson(Map<String, dynamic> json) {
    return CommunityModel(
      id: json['id'] as String,
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      type: json['type'] as String? ?? 'MIXTO',
      visibility: json['visibility'] as String? ?? 'PUBLICA',
      createdBy: json['createdBy'] as String,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String).toLocal()
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String).toLocal()
          : null,
      status: json['status'] as String? ?? 'ACTIVA',
      memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
      isMember: json['isMember'] as bool? ?? false,
      userRole: json['userRole'] as String?,
      membershipStatus: json['membershipStatus'] as String?,
    );
  }

  /// Método para serializar de objeto Dart a JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'type': type,
      'visibility': visibility,
      'createdBy': createdBy,
      'createdAt': createdAt?.toUtc().toIso8601String(),
      'updatedAt': updatedAt?.toUtc().toIso8601String(),
      'status': status,
      'memberCount': memberCount,
      'isMember': isMember,
      'userRole': userRole,
    };
  }
}

class CommunityCreateRequestModel {
  final String name;
  final String? description;
  final String type; // LIBROS, MUSICA, MIXTO
  final String visibility; // PUBLICA, PRIVADA

  const CommunityCreateRequestModel({
    required this.name,
    this.description,
    required this.type,
    required this.visibility,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'description': description,
      'type': type,
      'visibility': visibility,
    };
  }
}

class CommunityUpdateRequestModel {
  final String? description;
  final String? type;
  final String? visibility;

  const CommunityUpdateRequestModel({
    this.description,
    this.type,
    this.visibility,
  });

  Map<String, dynamic> toJson() {
    return {
      if (description != null) 'description': description,
      if (type != null) 'type': type,
      if (visibility != null) 'visibility': visibility,
    };
  }
}