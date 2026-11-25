package com.musa.social.repository;

import com.musa.social.entity.ModerationTerm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModerationTermRepository
        extends JpaRepository<ModerationTerm, UUID> {

    List<ModerationTerm> findByActiveTrue();
}