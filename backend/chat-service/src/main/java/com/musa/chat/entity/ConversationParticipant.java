package com.musa.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants", schema = "public")
@IdClass(ConversationParticipantId.class)
@Getter
@Setter
@NoArgsConstructor
public class ConversationParticipant {
    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void initializeJoinedAt() {
        if (joinedAt == null) joinedAt = Instant.now();
    }
}
