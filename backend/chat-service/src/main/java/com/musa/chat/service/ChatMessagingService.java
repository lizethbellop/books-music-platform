package com.musa.chat.service;

import com.musa.chat.client.UsersClient;
import com.musa.chat.dto.request.SendMessageRequest;
import com.musa.chat.dto.response.*;
import com.musa.chat.entity.*;
import com.musa.chat.exception.*;
import com.musa.chat.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatMessagingService {
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final MessageRepository messages;
    private final UsersClient users;
    private final ApplicationEventPublisher events;
    public ChatMessagingService(ConversationRepository conversations, ConversationParticipantRepository participants,
            MessageRepository messages, UsersClient users, ApplicationEventPublisher events) {
        this.conversations = conversations; this.participants = participants;
        this.messages = messages; this.users = users; this.events = events;
    }
    @Transactional(readOnly = true)
    public ConversationListResponse list(UUID userId, int page, int size) {
        validateSize(size);
        if (page < 0 || page > 100000) throw new InvalidChatRequestException("Página no válida.");
        users.requireActiveParticipants(userId, userId);
        var slice = conversations.findActiveForUser(userId, PageRequest.of(page, size));
        var items = slice.getContent().stream().map(c -> new ConversationResponse(c.getId(), c.getType(),
                c.getCreatedAt(), participantIds(c.getId()), c.getName(), c.getOwnerId())).toList();
        return new ConversationListResponse(items, slice.hasNext(), slice.hasNext() ? page + 1 : null);
    }
    @Transactional(readOnly = true)
    public MessageHistoryResponse history(UUID userId, UUID conversationId, int size, Instant beforeAt, UUID beforeId) {
        validateSize(size);
        if ((beforeAt == null) != (beforeId == null)) throw new InvalidChatRequestException("Envía juntos beforeCreatedAt y beforeId.");
        var conversation = conversations.findById(conversationId).orElseThrow(ConversationNotFoundException::new);
        var membership = requireMember(userId, conversation);
        users.requireActiveParticipants(userId, userId);
        var page = PageRequest.of(0, size);
        org.springframework.data.domain.Slice<Message> slice;
        if (conversation.getType() == ConversationType.GROUP) {
            slice = beforeAt == null ? messages.findGroupHistory(conversationId, membership.getJoinedAt(), page)
                    : messages.findGroupHistoryBefore(conversationId, membership.getJoinedAt(), beforeAt, beforeId, page);
        } else {
            slice = beforeAt == null ? messages.findByConversationIdOrderByCreatedAtDescIdDesc(conversationId, page)
                    : messages.findHistoryBefore(conversationId, beforeAt, beforeId, page);
        }
        var items = slice.getContent().stream().map(MessageResponse::from).toList();
        var last = items.isEmpty() ? null : items.getLast();
        return new MessageHistoryResponse(items, slice.hasNext(), slice.hasNext() ? last.createdAt() : null,
                slice.hasNext() ? last.id() : null);
    }
    @Transactional
    public MessageResponse send(UUID userId, UUID conversationId, SendMessageRequest request) {
        // Serializar envío y cambios de miembros sobre la misma conversación.
        var conversation = conversations.findLocked(conversationId).orElseThrow(ConversationNotFoundException::new);
        requireMember(userId, conversation);
        String content = request.content();
        if (content == null || content.isBlank() || content.length() > 4000)
            throw new InvalidChatRequestException("Escribe un mensaje de entre 1 y 4000 caracteres.");
        List<UUID> ids = participantIds(conversationId);
        List<UUID> recipients;
        if (conversation.getType() == ConversationType.PRIVATE) {
            if (ids.size() != 2) throw new InvalidChatRequestException("La conversación privada no es válida.");
            UUID recipient = ids.stream().filter(id -> !id.equals(userId)).findFirst().orElseThrow(ConversationNotFoundException::new);
            users.requireActiveParticipants(userId, recipient); recipients = ids;
        } else {
            var active = users.findActiveUsers(userId, ids);
            recipients = ids.stream().filter(id -> active.containsKey(id.toString())).toList();
        }
        Message message = new Message(); message.setConversationId(conversationId);
        message.setSenderId(userId); message.setContent(content);
        MessageResponse response = MessageResponse.from(messages.saveAndFlush(message));
        events.publishEvent(new MessageCreatedEvent(response, recipients));
        return response;
    }
    private ConversationParticipant requireMember(UUID userId, Conversation conversation) {
        if (conversation.getClosedAt() != null) throw new ConversationNotFoundException();
        return participants.findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), userId)
                .orElseThrow(ConversationNotFoundException::new);
    }
    private List<UUID> participantIds(UUID id) {
        return participants.findByConversationIdAndLeftAtIsNullOrderByUserIdAsc(id).stream()
                .map(ConversationParticipant::getUserId).toList();
    }
    private void validateSize(int size) {
        if (size < 1 || size > 50) throw new InvalidChatRequestException("El tamaño debe estar entre 1 y 50.");
    }
}
