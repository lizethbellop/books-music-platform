package com.musa.communities_service.polls.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "community_poll_votes")
public class PollVote {

    @Id
    private UUID id;

    @Column(name = "poll_id", nullable = false, updatable = false)
    private UUID pollId;

    @Column(name = "option_id", nullable = false, updatable = false)
    private UUID optionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PollVote() {
    }

    public PollVote(UUID pollId, UUID optionId, UUID userId) {
        this.id = UUID.randomUUID();
        this.pollId = pollId;
        this.optionId = optionId;
        this.userId = userId;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPollId() {
        return pollId;
    }

    public UUID getOptionId() {
        return optionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}