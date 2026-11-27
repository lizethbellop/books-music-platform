package com.musa.chat.repository;
import com.musa.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;
public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query("""
        SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.createdAt >= :joinedAt
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    Slice<Message> findGroupHistory(@Param("conversationId") UUID conversationId,
            @Param("joinedAt") Instant joinedAt, Pageable pageable);

    @Query("""
        SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.createdAt >= :joinedAt
          AND (m.createdAt < :beforeAt OR (m.createdAt = :beforeAt AND m.id < :beforeId))
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    Slice<Message> findGroupHistoryBefore(@Param("conversationId") UUID conversationId,
            @Param("joinedAt") Instant joinedAt, @Param("beforeAt") Instant beforeAt,
            @Param("beforeId") UUID beforeId, Pageable pageable);

    Slice<Message> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, Pageable pageable);
    @Query("""
        SELECT m FROM Message m WHERE m.conversationId = :conversationId
          AND (m.createdAt < :beforeAt OR (m.createdAt = :beforeAt AND m.id < :beforeId))
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    Slice<Message> findHistoryBefore(@Param("conversationId") UUID conversationId,
            @Param("beforeAt") Instant beforeAt, @Param("beforeId") UUID beforeId, Pageable pageable);
}
