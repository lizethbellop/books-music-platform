package com.musa.profile.repository;

import com.musa.profile.entity.ListElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT COUNT(e)
            FROM ListElement e
            WHERE e.listId IN (
                SELECT l.id
                FROM UserList l
                WHERE l.profileId = :profileId
            )
            """)
    long countByProfileId(
            @Param("profileId") UUID profileId
    );
}