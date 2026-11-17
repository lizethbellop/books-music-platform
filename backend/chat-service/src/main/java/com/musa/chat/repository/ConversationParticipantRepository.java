package com.musa.chat.repository;

import com.musa.chat.entity.ConversationParticipant;
import com.musa.chat.entity.ConversationParticipantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
    List<ConversationParticipant> findByConversationIdOrderByUserIdAsc(UUID conversationId);
    Page<ConversationParticipant> findByUserIdOrderByJoinedAtDescConversationIdDesc(
            UUID userId, Pageable pageable);
}
