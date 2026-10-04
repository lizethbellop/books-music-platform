package com.musa.users.service.impl;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import com.musa.users.exception.PasswordMismatchException;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.exception.UserAlreadyExistsException;
import com.musa.users.repository.RoleRepository;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegisterServiceImpl registerService;

    private RegisterRequestDto request;

    // Instancia un DTO base con datos válidos antes de ejecutar cada @Test para evitar duplicar código
    @BeforeEach
    void setUp() {
        // DTO con contraseñas coincidentes para el flujo feliz y pruebas estándar
        request = new RegisterRequestDto("Ana López", "ana@usi.com", "Password123!", "Password123!", "USER");
    }

    @Test
    @DisplayName("Debe lanzar PasswordMismatchException si las contraseñas no coinciden")
    void register_debeLanzarExcepcion_siContrasenasNoCoinciden() {
        // Arrange
        RegisterRequestDto requestMismatch = new RegisterRequestDto(
                "Ana López",
                "ana@usi.com",
                "Password123!",
                "DiferentePassword!",
                "USER"
        );

        // Act & Assert
        assertThrows(PasswordMismatchException.class, () -> registerService.register(requestMismatch));

        // Verify: No debe interactuar con repositorios ni servicios si falla la primera validación
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder, emailService);
    }

    @Test
    @DisplayName("Debe lanzar UserAlreadyExistsException si el correo ya existe")
    void register_debeLanzarExcepcion_siCorreoYaExiste() {
        // Act: Configura el mock para simular una respuesta específica cuando se invoque este método.
        when(userRepository.existsByEmail("ana@usi.com")).thenReturn(true);

        // Assert: Verifica que la ejecución del lambda lance la excepción esperada; si no la lanza, la prueba falla.
        assertThrows(UserAlreadyExistsException.class, () -> registerService.register(request));

        verify(userRepository).existsByEmail("ana@usi.com");
        verifyNoInteractions(roleRepository, passwordEncoder, emailService);//Confirma que el flujo se detuvo tempranamente y nunca llegó a tocar a estos servicios o repositorios.
        verify(userRepository, never()).save(any());// Garantiza estrictamente que un método crítico (como guardar en BD) jamás fue ejecutado.
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el rol no existe")
    void register_debeLanzarExcepcion_siRolNoExiste() {
        // Arrange
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> registerService.register(request));

        verify(userRepository).existsByEmail(request.email());
        verify(roleRepository).findByName("USER");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, emailService);
    }

    @Test
    @DisplayName("Debe registrar usuario y enviar correo de bienvenida si todos los datos son válidos")
    void register_debeRegistrarUsuario_siDatosSonValidos() {
        // Arrange
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123!")).thenReturn("HASH_SIMULADO");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponseDto response = registerService.register(request);

        // Assert:  // Compara el resultado retornado contra el mensaje o valor de negocio esperado.
        assertEquals("Registro exitoso.", response.message());

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("ana@usi.com", "Ana López");
    }

    @Test
    @DisplayName("Debe construir y guardar la entidad User con sus atributos correctos")
    void register_debeGuardarUsuarioConAtributosCorrectos() {
        // Arrange
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("HASH_SIMULADO");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));// Simula un comportamiento dinámico en el mock (aquí imita a JPA devolviendo el mismo objeto User que recibe para guardar).

        // Act
        registerService.register(request);

        // Assert: Intercepta y "atrapa" el objeto interno creado dentro del servicio para poder inspeccionar sus atributos.
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertAll(
                () -> assertEquals("Ana López", savedUser.getFullName()),
                () -> assertEquals("ana@usi.com", savedUser.getEmail()),
                () -> assertEquals("HASH_SIMULADO", savedUser.getPasswordHash()),
                () -> assertSame(role, savedUser.getRole()),
                () -> assertTrue(savedUser.getIsActive())
        );
    }
}