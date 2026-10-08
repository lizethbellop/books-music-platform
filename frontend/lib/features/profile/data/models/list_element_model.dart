class ListElementModel {
  final String id;
  final String elementType;
  final String referenceId;
  final String resolutionStatus;
  final Map<String, dynamic>? content;

  const ListElementModel({
    required this.id,
    required this.elementType,
    required this.referenceId,
    this.resolutionStatus = 'UNAVAILABLE',
    this.content,
  });

  factory ListElementModel.fromJson(Map<String, dynamic> json) {
    return ListElementModel(
      id: json['id'] as String,
      elementType: json['elementType'] as String,
      referenceId: json['referenceId'] as String,
      resolutionStatus:
          json['resolutionStatus'] as String? ?? 'UNAVAILABLE',
      content: json['content'] as Map<String, dynamic>?,
    );
  }

  bool get isAvailable =>
      resolutionStatus == 'AVAILABLE' && content != null;

  String get title {
    final key = elementType == 'BOOK' ? 'title' : 'name';
    final value = content?[key];

    if (value is String && value.trim().isNotEmpty) {
      return value.trim();
    }

    return switch (elementType) {
      'BOOK' => 'Libro',
      'SONG' => 'Canción',
      'ARTIST' => 'Artista',
      _ => 'Contenido',
    };
  }

  String? get subtitle {
    if (elementType == 'BOOK') {
      final authors = content?['authors'];

      if (authors is! List) return null;

      final names = authors
          .whereType<String>()
          .map((name) => name.trim())
          .where((name) => name.isNotEmpty)
          .join(', ');

      return names.isEmpty ? null : names;
    }

    if (elementType == 'SONG') {
      final artist = content?['artistName'];

      if (artist is String && artist.trim().isNotEmpty) {
        return artist.trim();
      }
    }

    return null;
  }

  String? get imageUrl {
    final key = elementType == 'BOOK' ? 'coverUrl' : 'imageUrl';
    final value = content?[key];

    if (value is String && value.trim().isNotEmpty) {
      return value.trim();
    }

    return null;
  }

  String? get statusMessage {
    if (isAvailable) return null;

    return resolutionStatus == 'NOT_FOUND'
        ? 'Este contenido ya no está disponible.'
        : 'No se pudo cargar este contenido. Intenta nuevamente.';
  }
}