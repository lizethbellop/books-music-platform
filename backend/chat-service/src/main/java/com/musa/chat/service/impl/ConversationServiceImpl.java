package com.musa.chat.service.impl;

import com.musa.chat.client.UsersClient;
import com.musa.chat.dto.request.CreatePrivateConversationRequest;
import com.musa.chat.dto.response.ConversationResponse;
import com.musa.chat.entity.Conversation;
import com.musa.chat.entity.ConversationParticipant;
import com.musa.chat.entity.ConversationType;
import com.musa.chat.exception.SelfConversationNotAllowedException;
import com.musa.chat.repository.ConversationParticipantRepository;
import com.musa.chat.repository.ConversationRepository;
import com.musa.chat.repository.PrivateConversationLock;
import com.musa.chat.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final PrivateConversationLock privateConversationLock;
    private final UsersClient usersClient;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            PrivateConversationLock privateConversationLock,
            UsersClient usersClient
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.privateConversationLock = privateConversationLock;
        this.usersClient = usersClient;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConversationResponse createPrivateConversation(
            UUID currentUserId,
            CreatePrivateConversationRequest request
    ) {
        UUID recipientId = request.recipientId();

        if (currentUserId.equals(recipientId)) {
            throw new SelfConversationNotAllowedException();
        }

        usersClient.requireActiveParticipants(currentUserId, recipientId);
        privateConversationLock.acquire(currentUserId, recipientId);

        Conversation conversation = conversationRepository
                .findPrivateConversationBetween(
                        currentUserId,
                        recipientId,
                        ConversationType.PRIVATE
                )
                .orElseGet(() ->
                        createConversationWithParticipants(
                                currentUserId,
                                recipientId
                        )
                );

        return toResponse(conversation);
    }

    private Conversation createConversationWithParticipants(
            UUID firstUserId,
            UUID secondUserId
    ) {
        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);

        Conversation savedConversation =
                conversationRepository.save(conversation);

        participantRepository.saveAll(List.of(
                createParticipant(savedConversation.getId(), firstUserId),
                createParticipant(savedConversation.getId(), secondUserId)
        ));

        return savedConversation;
    }

    private ConversationParticipant createParticipant(
            UUID conversationId,
            UUID userId
    ) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);

        return participant;
    }

    private ConversationResponse toResponse(Conversation conversation) {
        List<UUID> participantIds = participantRepository
                .findByConversationIdOrderByUserIdAsc(conversation.getId())
                .stream()
                .map(ConversationParticipant::getUserId)
                .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getCreatedAt(),
                participantIds
        );
    }
}