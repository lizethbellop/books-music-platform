package com.musa.notifications.service;
import com.musa.notifications.dto.*;
import com.musa.notifications.exception.*;
import com.musa.notifications.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class NotificationInboxService {
    private final NotificationRepository repository;
    public NotificationInboxService(NotificationRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public NotificationPage list(UUID recipient, int page, int size, boolean unreadOnly) {
        if (page < 0 || page > 100000 || size < 1 || size > 50) throw new InvalidNotificationQueryException();
        var pageable = PageRequest.of(page, size);
        var slice = unreadOnly
                ? repository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(recipient, pageable)
                : repository.findByRecipientIdOrderByCreatedAtDescIdDesc(recipient, pageable);
        return new NotificationPage(slice.getContent().stream().map(NotificationResponse::from).toList(),
                slice.hasNext(), slice.hasNext() ? page + 1 : null);
    }
    @Transactional(readOnly = true)
    public long unreadCount(UUID recipient) { return repository.countByRecipientIdAndReadAtIsNull(recipient); }
    @Transactional
    public void markRead(UUID recipient, UUID id) {
        if (repository.markReadForRecipient(id, recipient) == 0) throw new NotificationNotFoundException();
    }
    @Transactional
    public int markAllRead(UUID recipient) { return repository.markAllReadForRecipient(recipient); }
}
