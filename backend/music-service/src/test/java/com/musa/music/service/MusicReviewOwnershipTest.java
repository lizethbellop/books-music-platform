package com.musa.music.service;

import com.musa.music.dto.MusicReviewUpdateRequest;
import com.musa.music.entity.MusicReview;
import com.musa.music.repository.MusicReviewRepository;
import com.musa.music.repository.MusicRatingRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MusicReviewOwnershipTest {
    @Mock MusicReviewRepository repository;
    @Mock MusicContentService content;
    @Mock MusicRatingRepository ratings;
    @InjectMocks MusicReviewService service;

    @Test void ownerCanEditReview() {
        UUID owner = UUID.randomUUID();
        MusicReview review = MusicReview.builder().id(7L).userId(owner).reviewText("Antes").build();
        when(repository.findById(7L)).thenReturn(Optional.of(review));
        when(repository.save(review)).thenReturn(review);
        service.updateReview(7L, owner, new MusicReviewUpdateRequest("Después"));
        assertEquals("Después", review.getReviewText());
        verify(repository).save(review);
    }

    @Test void otherUserCannotEditReview() {
        MusicReview review = MusicReview.builder().id(7L).userId(UUID.randomUUID()).reviewText("Antes").build();
        when(repository.findById(7L)).thenReturn(Optional.of(review));
        assertThrows(AccessDeniedException.class, () -> service.updateReview(
                7L, UUID.randomUUID(), new MusicReviewUpdateRequest("Después")));
        assertEquals("Antes", review.getReviewText());
        verify(repository, never()).save(any());
    }

    @Test void ownerCanDeleteReview() {
        UUID owner = UUID.randomUUID();
        MusicReview review = MusicReview.builder().id(7L).userId(owner).build();
        when(repository.findById(7L)).thenReturn(Optional.of(review));
        service.deleteReview(7L, owner);
        verify(repository).delete(review);
    }

    @Test void otherUserCannotDeleteReview() {
        MusicReview review = MusicReview.builder().id(7L).userId(UUID.randomUUID()).build();
        when(repository.findById(7L)).thenReturn(Optional.of(review));
        assertThrows(AccessDeniedException.class, () -> service.deleteReview(7L, UUID.randomUUID()));
        verify(repository, never()).delete(any());
    }

    @Test void missingReviewReturnsNotFound() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class,
                () -> service.deleteReview(7L, UUID.randomUUID()));
        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).delete(any());
    }
}
