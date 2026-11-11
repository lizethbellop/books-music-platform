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

    @Query("""
            select distinct e.referenceId from ListElement e
            where e.elementType = 'BOOK' and e.listId in
                (select l.id from UserList l where l.profileId = :profileId)
            order by e.referenceId
            """)
    List<String> findBookReferencesByProfileId(@Param("profileId") UUID profileId);

    boolean existsByListIdAndElementTypeAndReferenceId(
            UUID listId,
            String elementType,
            String referenceId
    );
}