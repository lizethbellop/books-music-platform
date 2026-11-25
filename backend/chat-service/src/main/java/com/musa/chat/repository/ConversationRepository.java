package com.musa.chat.repository;

import com.musa.chat.entity.Conversation;
import com.musa.chat.entity.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository
        extends JpaRepository<Conversation, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.id = :id")
    Optional<Conversation> findLocked(@Param("id") UUID id);

    @Query("""
        SELECT c FROM Conversation c WHERE c.closedAt IS NULL AND EXISTS (
            SELECT p FROM ConversationParticipant p WHERE p.conversationId = c.id
                AND p.userId = :userId AND p.leftAt IS NULL)
        ORDER BY c.createdAt DESC, c.id DESC
        """)
    org.springframework.data.domain.Slice<Conversation> findActiveForUser(@Param("userId") UUID userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT c FROM Conversation c WHERE c.type = :type AND EXISTS (
            SELECT p FROM ConversationParticipant p
            WHERE p.conversationId = c.id AND p.userId = :userId)
        ORDER BY c.createdAt DESC, c.id DESC
        """)
    org.springframework.data.domain.Slice<Conversation> findForUser(
            @Param("userId") UUID userId, @Param("type") ConversationType type,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE c.type = :type
              AND EXISTS (
                  SELECT p
                  FROM ConversationParticipant p
                  WHERE p.conversationId = c.id
                    AND p.userId = :firstUserId
              )
              AND EXISTS (
                  SELECT p
                  FROM ConversationParticipant p
                  WHERE p.conversationId = c.id
                    AND p.userId = :secondUserId
              )
              AND (
                  SELECT COUNT(p)
                  FROM ConversationParticipant p
                  WHERE p.conversationId = c.id
              ) = 2
            """)
    Optional<Conversation> findPrivateConversationBetween(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId,
            @Param("type") ConversationType type
    );
}