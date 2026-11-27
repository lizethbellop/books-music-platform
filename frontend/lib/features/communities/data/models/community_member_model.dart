class CommunityMemberModel {
  final String id;
  final String communityId;
  final String userId;
  final String role; // ADMINISTRADOR, MIEMBRO
  final DateTime joinedAt;
  final String status; // ACTIVO, PENDIENTE, RETIRADO

  const CommunityMemberModel({
    required this.id,
    required this.communityId,
    required this.userId,
    required this.role,
    required this.joinedAt,
    required this.status,
  });

  bool get isPending => status == 'PENDIENTE';
  bool get isActive => status == 'ACTIVO';
  bool get isAdmin => role == 'ADMINISTRADOR';

  factory CommunityMemberModel.fromJson(Map<String, dynamic> json) {
    return CommunityMemberModel(
      id: json['id'] as String,
      communityId: json['communityId'] as String,
      userId: json['userId'] as String,
      role: json['role'] as String? ?? 'MIEMBRO',
      joinedAt: DateTime.parse(json['joinedAt'] as String).toLocal(),
      status: json['status'] as String? ?? 'ACTIVO',
    );
  }

  /// Método para serializar de objeto Dart a JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'communityId': communityId,
      'userId': userId,
      'role': role,
      'joinedAt': joinedAt.toUtc().toIso8601String(),
      'status': status,
    };
  }
}