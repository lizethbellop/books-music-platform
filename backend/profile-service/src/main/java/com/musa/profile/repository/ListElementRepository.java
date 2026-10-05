package com.musa.profile.repository;

import com.musa.profile.entity.ListElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListElementRepository
        extends JpaRepository<ListElement, UUID> {

    List<ListElement> findByListId(UUID listId);

    boolean existsByListIdAndElementTypeAndReferenceId(
            UUID listId,
            String elementType,
            String referenceId
    );
}