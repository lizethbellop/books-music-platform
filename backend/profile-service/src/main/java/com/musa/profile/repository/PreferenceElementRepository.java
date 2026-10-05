package com.musa.profile.repository;

import com.musa.profile.entity.PreferenceElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PreferenceElementRepository
        extends JpaRepository<PreferenceElement, UUID> {

    List<PreferenceElement> findByPreferenceId(UUID preferenceId);

    boolean existsByPreferenceIdAndElementTypeAndReferenceId(
            UUID preferenceId,
            String elementType,
            String referenceId
    );
}