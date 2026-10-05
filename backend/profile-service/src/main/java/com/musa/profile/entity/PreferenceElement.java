package com.musa.profile.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "preference_elements")
public class PreferenceElement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "preference_id", nullable = false)
    private UUID preferenceId;

    @Column(name = "element_type", nullable = false, length = 20)
    private String elementType;

    @Column(name = "reference_id", nullable = false, length = 255)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PreferenceElement() {
    }

    public PreferenceElement(UUID preferenceId, String elementType, String referenceId) {
        this.preferenceId = preferenceId;
        this.elementType = elementType;
        this.referenceId = referenceId;
    }

    @PrePersist
    void beforeInsert() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPreferenceId() {
        return preferenceId;
    }

    public String getElementType() {
        return elementType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
