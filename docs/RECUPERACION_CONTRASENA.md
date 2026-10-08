# Recuperar contraseña

Se integraron desde feature/users-login las tres pantallas de Derly, su modelo VerifyTokenRequestModel y la verificación de códigos en el servicio de Usuarios. Se conectaron las rutas y el botón del login conservando AuthSessionManager, Recordarme, refresh-token y almacenamiento seguro actuales. La llamada HTTP usa el manejo de errores y límite de espera comunes del cliente.

## Probar manualmente

1. Encender PostgreSQL, Redis y los cuatro backend. Abrir Flutter.
2. En el login, pulsar **¿Olvidaste tu contraseña?**.
3. Escribir el correo de una cuenta existente y pulsar **Enviar token**.
4. Revisar ese correo (también spam), copiar el código completo y pegarlo en **Verificar token**. El código dura **5 minutos** desde su creación. Si venció, regresar y solicitar uno nuevo.
5. Escribir una contraseña nueva y su confirmación. Debe tener al menos 8 caracteres, mayúscula, minúscula, número y un carácter permitido como !; debe ser diferente de la anterior.
6. Pulsar **Cambiar contraseña**. La aplicación regresa al login. Entrar con el correo y la contraseña nueva.

La respuesta de solicitud es genérica incluso si el correo no existe. Eso no implica que se haya enviado un mensaje a una cuenta inexistente. Para recibir el correo real, Usuarios necesita MAIL_USERNAME y MAIL_PASSWORD correctos en su .env.

## Comunicación

- POST /api/v1/auth/forgot-password: solicita el correo.
- POST /api/v1/auth/verify-token: verifica el código sin consumirlo.
- POST /api/v1/auth/reset-password: vuelve a verificar el código, cambia la contraseña y elimina el código para impedir reutilización.

El código de recuperación no es el JWT de acceso ni el refresh token. Este flujo no cambia el UUID ni las listas, preferencias, reseñas o conexiones entre servicios. No requiere migración nueva: utiliza password_resets existente. Si se abrió el formulario pero el código venció mientras se escribía la contraseña, el backend rechazará el cambio y habrá que solicitar otro.

## Pruebas

Se añadieron casos de servicio y controlador para verificación correcta, código vacío, inexistente y vencido. La integración PostgreSQL/HTTP simula el envío de correo y prueba solicitud, verificación, cambio, rechazo de reutilización y login con la contraseña nueva. Los tests Flutter prueban el enlace desde login, navegación completa y permanencia en la pantalla ante código rechazado.

No se enviaron correos reales durante las pruebas automatizadas. El envío SMTP real queda para el recorrido manual con tu cuenta.
