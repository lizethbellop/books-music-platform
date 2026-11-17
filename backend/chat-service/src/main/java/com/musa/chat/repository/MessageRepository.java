package com.musa.chat.repository;

import com.musa.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Slice<Message> findByConversationIdOrderByCreatedAtDescIdDesc(
            UUID conversationId, Pageable pageable);
}
