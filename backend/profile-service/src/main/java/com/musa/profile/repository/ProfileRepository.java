package com.musa.profile.repository;

import com.musa.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);
    @Modifying
    @Query(value = """
        INSERT INTO profiles (
            id,
            user_id,
            is_private,
            created_at,
            updated_at
        )
        VALUES (
            :id,
            :userId,
            false,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (user_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );
}
