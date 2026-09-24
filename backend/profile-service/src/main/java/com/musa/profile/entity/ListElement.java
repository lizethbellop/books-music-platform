package com.musa.profile.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "list_elements")
public class ListElement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "list_id", nullable = false)
    private UUID listId;

    @Column(name = "element_type", nullable = false, length = 20)
    private String elementType;

    @Column(name = "reference_id", nullable = false, length = 255)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ListElement() {
    }

    public ListElement(UUID listId, String elementType, String referenceId) {
        this.listId = listId;
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

    public UUID getListId() {
        return listId;
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
