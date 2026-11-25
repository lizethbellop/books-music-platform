package com.musa.communities_service.polls.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "community_polls")
public class Poll {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false, updatable = false)
    private UUID communityId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 300, updatable = false)
    private String question;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closes_at", nullable = false, updatable = false)
    private OffsetDateTime closesAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    protected Poll() {
    }

    public Poll(
            UUID communityId,
            UUID createdBy,
            String question,
            OffsetDateTime closesAt
    ) {
        this.id = UUID.randomUUID();
        this.communityId = communityId;
        this.createdBy = createdBy;
        this.question = question;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.closesAt = closesAt;
    }

    public void close(OffsetDateTime now) {
        if (closedAt == null) {
            closedAt = now;
        }
    }

    public boolean isOpenAt(OffsetDateTime now) {
        return closedAt == null && now.isBefore(closesAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getQuestion() {
        return question;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getClosesAt() {
        return closesAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }
}