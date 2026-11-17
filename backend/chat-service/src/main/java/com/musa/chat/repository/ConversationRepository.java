package com.musa.chat.repository;

import com.musa.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
}
