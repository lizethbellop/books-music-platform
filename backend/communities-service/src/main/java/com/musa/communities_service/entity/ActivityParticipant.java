package com.musa.communities_service.entity;

import com.musa.communities_service.enums.ParticipantStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "activity_participants", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_activity_participant",
                columnNames = {"activity_id", "user_id"}
        )
})
public class ActivityParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private CommunityActivity activity;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private OffsetDateTime confirmedAt;

    protected ActivityParticipant() {
    }

    public ActivityParticipant(CommunityActivity activity, UUID userId, ParticipantStatus status) {
        this.activity = activity;
        this.userId = userId;
        this.status = status;
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PrePersist
    protected void beforeInsert() {
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.status == null) {
            this.status = ParticipantStatus.CONFIRMADA;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CommunityActivity getActivity() {
        return activity;
    }

    public void setActivity(CommunityActivity activity) {
        this.activity = activity;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(OffsetDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}