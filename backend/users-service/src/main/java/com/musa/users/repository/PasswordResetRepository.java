package com.musa.users.repository;

import com.musa.users.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Repositorio Spring Data JPA para la gestión de persistencia y consultas de la entidad PasswordReset. */
@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

    @Query("SELECT pr FROM PasswordReset pr JOIN FETCH pr.user u JOIN FETCH u.role WHERE pr.tokenHash = :tokenHash")
    Optional<PasswordReset> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    Optional<PasswordReset> findByTokenHash(String tokenHash);
}
