package com.musa.notifications.service;

import com.musa.notifications.dto.NotificationEvent;
import com.musa.notifications.entity.NotificationTargetType;
import com.musa.notifications.repository.NotificationRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final Validator validator;

    public NotificationService(
            NotificationRepository repository,
            Validator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public void process(NotificationEvent event) {
        var violations = validator.validate(event);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        if (event.actorId().equals(event.recipientId())) {
            return;
        }

        NotificationContent content = buildContent(event);

        repository.insertIfAbsent(
                event.eventId(),
                event.recipientId(),
                event.actorId(),
                event.type().name(),
                content.title(),
                content.body(),
                content.targetType().name(),
                event.targetId()
        );
    }

    private NotificationContent buildContent(NotificationEvent event) {
        return switch (event.type()) {
            case CHAT_MESSAGE_CREATED -> new NotificationContent(
                    "Nuevo mensaje",
                    "Tienes un nuevo mensaje.",
                    NotificationTargetType.CONVERSATION
            );

            case USER_FOLLOWED -> new NotificationContent(
                    "Nuevo seguidor",
                    "Una persona comenzó a seguirte.",
                    NotificationTargetType.PROFILE
            );

            case POST_LIKED -> new NotificationContent(
                    "Nuevo me gusta",
                    "A una persona le gustó tu publicación.",
                    NotificationTargetType.POST
            );

            case POST_COMMENTED -> new NotificationContent(
                    "Nuevo comentario",
                    "Una persona comentó tu publicación.",
                    NotificationTargetType.POST
            );

            case GROUP_MEMBER_ADDED -> new NotificationContent(
                    "Te agregaron a un grupo", "Ya puedes participar en la conversación grupal.", NotificationTargetType.CONVERSATION);
            case GROUP_MEMBER_REMOVED -> new NotificationContent(
                    "Saliste de un grupo", "La administración te retiró de una conversación grupal.", NotificationTargetType.CONVERSATION);
            case GROUP_ADMIN_CHANGED -> new NotificationContent(
                    "Administración de grupo", "Ahora administras una conversación grupal.", NotificationTargetType.CONVERSATION);
            case POST_PUBLISHED -> new NotificationContent(
                    "Nueva publicación",
                    "Una persona que sigues publicó contenido.",
                    NotificationTargetType.POST
            );
        };
    }

    private record NotificationContent(
            String title,
            String body,
            NotificationTargetType targetType
    ) {
    }
}