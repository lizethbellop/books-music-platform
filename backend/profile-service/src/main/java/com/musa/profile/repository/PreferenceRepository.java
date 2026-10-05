package com.musa.profile.repository;

import com.musa.profile.entity.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreferenceRepository extends JpaRepository<Preference, UUID> {

    Optional<Preference> findByProfileId(UUID profileId);
}
