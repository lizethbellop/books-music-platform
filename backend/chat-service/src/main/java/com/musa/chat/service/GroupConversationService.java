package com.musa.chat.service;

import com.musa.chat.client.UsersClient;
import com.musa.chat.dto.request.*;
import com.musa.chat.dto.response.ConversationResponse;
import com.musa.chat.entity.*;
import com.musa.chat.exception.*;
import com.musa.chat.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GroupConversationService {
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final UsersClient users;
    private final ApplicationEventPublisher events;
    public GroupConversationService(ConversationRepository conversations, ConversationParticipantRepository participants,
            UsersClient users, ApplicationEventPublisher events) {
        this.conversations = conversations; this.participants = participants; this.users = users; this.events = events;
    }
    @Transactional
    public ConversationResponse create(UUID actor, CreateGroupRequest request) {
        String name = validName(request.name());
        if (request.participantIds() == null || request.participantIds().isEmpty() || request.participantIds().size() > 49
                || request.participantIds().stream().anyMatch(Objects::isNull)) throw new InvalidChatRequestException("Elige de 1 a 49 personas.");
        var ids = new LinkedHashSet<>(request.participantIds());
        if (ids.size() != request.participantIds().size() || ids.contains(actor))
            throw new InvalidChatRequestException("Los participantes deben ser distintos; tu cuenta se agrega automáticamente.");
        ids.add(actor); users.requireActiveUsers(actor, ids);
        var group = new Conversation(); group.setType(ConversationType.GROUP); group.setName(name); group.setOwnerId(actor);
        group = conversations.saveAndFlush(group);
        Instant now = now();
        for (UUID id : ids) {
            var member = new ConversationParticipant(); member.setConversationId(group.getId());
            member.setUserId(id); member.setJoinedAt(now); participants.save(member);
        }
        participants.flush();
        changed(GroupChangedEvent.Action.CREATED, group, actor, request.participantIds(), ids);
        return response(group);
    }
    @Transactional(readOnly = true)
    public ConversationResponse details(UUID actor, UUID id) {
        var conversation = conversations.findById(id).orElseThrow(ConversationNotFoundException::new);
        requireMember(actor, conversation); users.requireActiveParticipants(actor, actor);
        return response(conversation);
    }
    @Transactional
    public ConversationResponse rename(UUID actor, UUID id, String name) {
        var group = groupLocked(actor, id, true); group.setName(validName(name));
        changed(GroupChangedEvent.Action.RENAMED, group, actor, List.of(), activeIds(id));
        return response(group);
    }
    @Transactional
    public ConversationResponse add(UUID actor, UUID id, UUID added) {
        var group = groupLocked(actor, id, true);
        if (added == null) throw new InvalidChatRequestException("Selecciona una persona.");
        var ids = activeIds(id);
        if (ids.contains(added)) throw new InvalidChatRequestException("Esta persona ya pertenece al grupo.");
        if (ids.size() >= 50) throw new InvalidChatRequestException("El grupo admite hasta 50 participantes.");
        users.requireActiveParticipants(actor, added);
        var key = new ConversationParticipantId(id, added);
        var member = participants.findById(key).orElseGet(ConversationParticipant::new);
        member.setConversationId(id); member.setUserId(added); member.setJoinedAt(now()); member.setLeftAt(null);
        participants.saveAndFlush(member);
        changed(GroupChangedEvent.Action.MEMBER_ADDED, group, actor, List.of(added), activeIds(id));
        return response(group);
    }
    @Transactional
    public ConversationResponse remove(UUID actor, UUID id, UUID removed) {
        var group = groupLocked(actor, id, true);
        if (actor.equals(removed)) throw new InvalidChatRequestException("Usa Salir del grupo para retirarte.");
        var audience = activeIds(id);
        var member = participants.findByConversationIdAndUserIdAndLeftAtIsNull(id, removed)
                .orElseThrow(ConversationNotFoundException::new);
        member.setLeftAt(now()); participants.flush();
        changed(GroupChangedEvent.Action.MEMBER_REMOVED, group, actor, List.of(removed), audience);
        return response(group);
    }
    @Transactional
    public ConversationResponse transfer(UUID actor, UUID id, UUID successor) {
        var group = groupLocked(actor, id, true);
        if (successor == null || successor.equals(actor)) throw new InvalidChatRequestException("Elige a otra persona del grupo.");
        if (!activeIds(id).contains(successor)) throw new InvalidChatRequestException("La nueva administración debe pertenecer al grupo.");
        users.requireActiveParticipants(actor, successor); group.setOwnerId(successor);
        changed(GroupChangedEvent.Action.OWNER_CHANGED, group, actor, List.of(successor), activeIds(id));
        return response(group);
    }
    @Transactional
    public void leave(UUID actor, UUID id) {
        var group = groupLocked(actor, id, false);
        var audience = activeIds(id);
        var member = participants.findByConversationIdAndUserIdAndLeftAtIsNull(id, actor).orElseThrow(ConversationNotFoundException::new);
        member.setLeftAt(now()); participants.flush();
        var remaining = participants.findByConversationIdAndLeftAtIsNullOrderByUserIdAsc(id);
        if (group.getOwnerId().equals(actor)) {
            if (remaining.isEmpty()) group.setClosedAt(now());
            else {
                var lookup = new ArrayList<>(activeIds(id)); lookup.add(actor);
                var available = users.findActiveUsers(actor, lookup);
                var next = remaining.stream().filter(p -> available.containsKey(p.getUserId().toString()))
                        .min(Comparator.comparing(ConversationParticipant::getJoinedAt).thenComparing(p -> p.getUserId().toString()));
                if (next.isPresent()) {
                    UUID successor = next.get().getUserId(); group.setOwnerId(successor);
                    changed(GroupChangedEvent.Action.OWNER_CHANGED, group, actor, List.of(successor), audience);
                } else {
                    // No dejar un grupo sin administración utilizable.
                    for (var p : remaining) p.setLeftAt(now());
                    group.setClosedAt(now()); participants.flush();
                }
            }
        }
        changed(group.getClosedAt() == null ? GroupChangedEvent.Action.LEFT : GroupChangedEvent.Action.CLOSED,
                group, actor, List.of(actor), audience);
    }
    private Conversation groupLocked(UUID actor, UUID id, boolean admin) {
        var group = conversations.findLocked(id).orElseThrow(ConversationNotFoundException::new);
        requireMember(actor, group);
        if (group.getType() != ConversationType.GROUP) throw new InvalidChatRequestException("Esta operación solo está disponible en grupos.");
        if (admin && !actor.equals(group.getOwnerId())) throw new GroupPermissionException();
        users.requireActiveParticipants(actor, actor); return group;
    }
    private void requireMember(UUID actor, Conversation conversation) {
        if (conversation.getClosedAt() != null || !participants.existsByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), actor))
            throw new ConversationNotFoundException();
    }
    public ConversationResponse response(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getType(), conversation.getCreatedAt(),
                activeIds(conversation.getId()), conversation.getName(), conversation.getOwnerId());
    }
    private List<UUID> activeIds(UUID id) {
        return participants.findByConversationIdAndLeftAtIsNullOrderByUserIdAsc(id).stream().map(ConversationParticipant::getUserId).toList();
    }
    private void changed(GroupChangedEvent.Action action, Conversation group, UUID actor, Collection<UUID> affected, Collection<UUID> audience) {
        events.publishEvent(new GroupChangedEvent(action, group.getId(), actor, List.copyOf(affected), List.copyOf(audience)));
    }
    private String validName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100)
            throw new InvalidChatRequestException("El nombre debe tener de 1 a 100 caracteres.");
        return name.strip();
    }
    private Instant now() { return Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
