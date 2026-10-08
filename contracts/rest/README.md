# Contratos REST

Los YAML describen el comportamiento objetivo acordado. No conectan servicios por sí mismos: backend, Flutter y las pruebas deben cumplirlos. No requieren un API Gateway.

## Autenticación

`autenticacion.yaml` contiene login, registro, renovación, logout, solicitud de recuperación y restablecimiento.

Mejoras aprobadas: access token de 15 minutos; límite absoluto de sesión de 7 días; rememberMe determina persistencia local; renovación automática y rotación atómica del refresh token; comprobar cuenta activa al renovar; respuesta uniforme para correo inexistente en login y recuperación; comprobar confirmación de contraseña al restablecer; token de recuperación de un solo uso. Sin rememberMe la aplicación no persiste tokens. En navegador, cerrar una pestaña no se puede tratar como garantía de revocación en servidor: el límite absoluto sigue aplicando.

El logout revoca la renovación de esa sesión y el cliente elimina sus tokens; el access token puede seguir válido hasta vencer. No equivale a cerrar todos los dispositivos.

## Estado

El contrato de autenticación está redactado; las mejoras todavía requieren implementación y pruebas. Perfil, libros, música, creación de perfil tras registro y validación del access token entre servicios siguen pendientes de completar. No se accede a bases de datos de otros servicios.

## Lectura

`paths` define operaciones y respuestas; `components.schemas` define mensajes reutilizables; `$ref` los conecta. `security: []` indica ausencia de requisito de access token, no ausencia de validación del cuerpo. Ningún ejemplo debe incluir secretos reales.
