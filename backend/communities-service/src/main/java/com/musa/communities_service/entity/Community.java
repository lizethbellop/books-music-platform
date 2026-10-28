package com.musa.communities_service.entity;

import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "communities")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityVisibility visibility;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityStatus status;

    protected Community() {
    }

    public Community(String name, String description, CommunityType type, CommunityVisibility visibility, UUID createdBy) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.visibility = visibility;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CommunityStatus.ACTIVA;
        }
    }

    @PreUpdate
    protected void beforeUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CommunityType getType() {
        return type;
    }

    public void setType(CommunityType type) {
        this.type = type;
    }

    public CommunityVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(CommunityVisibility visibility) {
        this.visibility = visibility;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CommunityStatus getStatus() {
        return status;
    }

    public void setStatus(CommunityStatus status) {
        this.status = status;
    }
}
