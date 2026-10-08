package com.musa.users.repository;

import com.musa.users.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
/** Repositorio Spring Data JPA para la gestión de persistencia y consultas de la entidad User. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);
}