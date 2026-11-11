package com.musa.profile.repository;

import com.musa.profile.entity.PreferenceElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PreferenceElementRepository
        extends JpaRepository<PreferenceElement, UUID> {

    List<PreferenceElement> findByPreferenceId(UUID preferenceId);

    @org.springframework.data.jpa.repository.Query("""
            select distinct e.referenceId from PreferenceElement e
            where e.elementType = 'BOOK' and e.preferenceId in
                (select p.id from Preference p where p.profileId = :profileId)
            order by e.referenceId
            """)
    List<String> findBookReferencesByProfileId(
            @org.springframework.data.repository.query.Param("profileId") UUID profileId);

    boolean existsByPreferenceIdAndElementTypeAndReferenceId(
            UUID preferenceId,
            String elementType,
            String referenceId
    );

    @Query("""
            SELECT COUNT(e)
            FROM PreferenceElement e
            WHERE e.preferenceId IN (
                SELECT p.id
                FROM Preference p
                WHERE p.profileId = :profileId
            )
            """)
    long countByProfileId(
            @Param("profileId") UUID profileId
    );
}