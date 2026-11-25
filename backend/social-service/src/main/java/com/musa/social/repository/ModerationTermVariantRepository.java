package com.musa.social.repository;

import com.musa.social.entity.ModerationTermVariant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModerationTermVariantRepository
        extends JpaRepository<ModerationTermVariant, UUID> {

    List<ModerationTermVariant> findByTermIdIn(
            List<UUID> termIds
    );
}