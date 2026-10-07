package com.musa.users.repository;

import com.musa.users.entity.Role;
import com.musa.users.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeGuardarYRecuperarUsuarioPorCorreo() {

        // ARRANGE: crear y persistir un rol
        Role role = new Role();
        role.setName("USER");

        role = roleRepository.saveAndFlush(role);

        // ARRANGE: crear el usuario
        User user = new User();
        user.setFullName("Ana López");
        user.setEmail("ana@usi.com");
        user.setPasswordHash("HASH_DE_PRUEBA");
        user.setRole(role);
        user.setIsActive(true);

        // ACT: guarda en PostgreSQL
        User savedUser = userRepository.saveAndFlush(user);

        UUID savedId = savedUser.getId();

        // ACT: Envia las operaciones pendientes a PostgreSQL.
        entityManager.flush();

        //ACT: Vacia el contexto de persistencia de Hibernate.
        entityManager.clear();

        // ACT: consulta mediante el repositorio
        Optional<User> result = userRepository.findByEmail("ana@usi.com");

        // ASSERT: verifica los datos recuperados
        assertTrue(result.isPresent());

        User recoveredUser = result.get();

        assertAll(() -> assertNotNull(savedUser.getId()),
                () -> assertEquals(savedId, recoveredUser.getId()),
                () -> assertEquals("Ana López", recoveredUser.getFullName()),
                () -> assertEquals("ana@usi.com", recoveredUser.getEmail()),
                () -> assertEquals("HASH_DE_PRUEBA", recoveredUser.getPasswordHash()),
                () -> assertEquals("USER", recoveredUser.getRole().getName()),
                () -> assertTrue(recoveredUser.getIsActive()),
                () -> assertNotNull(recoveredUser.getCreatedAt())
        );
    }
}