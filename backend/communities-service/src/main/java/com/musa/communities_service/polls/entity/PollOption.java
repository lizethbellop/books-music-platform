package com.musa.communities_service.polls.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "community_poll_options")
public class PollOption {

    @Id
    private UUID id;

    @Column(name = "poll_id", nullable = false, updatable = false)
    private UUID pollId;

    @Column(nullable = false, length = 150, updatable = false)
    private String label;

    @Column(nullable = false, updatable = false)
    private short position;

    protected PollOption() {
    }

    public PollOption(UUID pollId, String label, short position) {
        this.id = UUID.randomUUID();
        this.pollId = pollId;
        this.label = label;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPollId() {
        return pollId;
    }

    public String getLabel() {
        return label;
    }

    public short getPosition() {
        return position;
    }
}