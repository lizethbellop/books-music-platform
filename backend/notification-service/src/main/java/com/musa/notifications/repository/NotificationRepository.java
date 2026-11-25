package com.musa.notifications.repository;

import com.musa.notifications.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    Slice<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(
            UUID recipientId,
            Pageable pageable
    );

    Slice<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(
            UUID recipientId,
            Pageable pageable
    );

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(
            UUID id,
            UUID recipientId
    );

    boolean existsByEventIdAndRecipientId(
            UUID eventId,
            UUID recipientId
    );

    @Modifying
    @Query(value = """
        UPDATE public.notifications SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
        WHERE id = :id AND recipient_id = :recipientId
        """, nativeQuery = true)
    int markReadForRecipient(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query(value = """
        UPDATE public.notifications SET read_at = CURRENT_TIMESTAMP
        WHERE recipient_id = :recipientId AND read_at IS NULL
        """, nativeQuery = true)
    int markAllReadForRecipient(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query(value = """
        INSERT INTO public.notifications (
            id,
            event_id,
            recipient_id,
            actor_id,
            type,
            title,
            body,
            target_type,
            target_id
        )
        VALUES (
            gen_random_uuid(),
            :eventId,
            :recipientId,
            :actorId,
            :type,
            :title,
            :body,
            :targetType,
            :targetId
        )
        ON CONFLICT (event_id, recipient_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("recipientId") UUID recipientId,
            @Param("actorId") UUID actorId,
            @Param("type") String type,
            @Param("title") String title,
            @Param("body") String body,
            @Param("targetType") String targetType,
            @Param("targetId") UUID targetId
    );
}