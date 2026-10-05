class ProfileApiException implements Exception {
  final int statusCode;
  final String message;

  const ProfileApiException({
    required this.statusCode,
    required this.message,
  });

  factory ProfileApiException.fromStatusCode(int statusCode) {
    final message = switch (statusCode) {
      400 => 'Los datos enviados no son válidos.',
      401 => 'Debes iniciar sesión.',
      403 => 'No tienes permiso para realizar esta acción.',
      404 => 'No se encontró el recurso.',
      409 => 'Ese elemento ya existe.',
      >= 500 => 'El servidor no está disponible.',
      _ => 'Ocurrió un error al comunicarse con Perfil.',
    };

    return ProfileApiException(statusCode: statusCode, message: message);
  }

  @override
  String toString() => message;
}
