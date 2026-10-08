package com.musa.users.repository;

import com.musa.users.entity.PasswordReset;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Transactional
public class PasswordResetRepositoryIntegrationTest {

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeGuardarYRecuperarPasswordResetPorTokenHashConUsuarioYRol() {
        // ARRANGE: crear y persistir un rol
        Role role = new Role();
        role.setName("USER");

        role = roleRepository.saveAndFlush(role);

        // ARRANGE: Crear y persistir el Usuario asociado
        User user = new User();
        user.setUsername("test_" + java.util.UUID.randomUUID().toString().replace("-", ""));
        user.setFullName("Carlos Gómez");
        user.setEmail("carlos@usi.com");
        user.setPasswordHash("HASH_DE_PRUEBA");
        user.setRole(role);
        user.setIsActive(true);

        user = userRepository.saveAndFlush(user);

        // ARRANGE: Crear la entidad passwordHash
        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setUser(user);
        passwordReset.setTokenHash("HASH_DE_PRUEBA");
        passwordReset.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        // ACT: guarda en PostgreSQL
        PasswordReset savedReset = passwordResetRepository.saveAndFlush(passwordReset);

        // ACT: Envía las operaciones pendientes a PostgreSQL.
        entityManager.flush();

        //ACT: Vacía el contexto de persistencia de Hibernate.
        entityManager.clear();

        // ACT: consulta mediante el repositorio
        Optional<PasswordReset> result = passwordResetRepository.findByTokenHashWithUser("HASH_DE_PRUEBA");

        // ASSERT: verifica los datos recuperados
        assertTrue(result.isPresent());

        PasswordReset recoveredReset = result.get();

        assertAll(
                () -> assertNotNull(recoveredReset.getId()),
                () -> assertEquals("HASH_DE_PRUEBA", recoveredReset.getTokenHash()),
                () -> assertEquals("carlos@usi.com", recoveredReset.getUser().getEmail()),
                () -> assertEquals("USER", recoveredReset.getUser().getRole().getName()),
                () -> assertNotNull(recoveredReset.getCreatedAt())
        );
    }
}
