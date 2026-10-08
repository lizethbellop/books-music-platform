package com.musa.profile.service;

import com.musa.profile.dto.*;
import com.musa.profile.dto.catalog.*;
import com.musa.profile.repository.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreferenceResolutionTest {
    @Test void resolvesPreferencesThroughCatalogAndPreservesUnavailableReferences() {
        CatalogLookupService catalog = mock(CatalogLookupService.class);
        PreferenceService service = spy(new PreferenceService(mock(ProfileService.class),
                mock(PreferenceRepository.class), mock(PreferenceElementRepository.class), catalog));
        UUID user = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        doReturn(new PreferencesResponse(List.of(
                new PreferenceElementResponse(bookId, "BOOK", "OL1W"),
                new PreferenceElementResponse(songId, "SONG", "song-id"))))
                .when(service).getOwnPreferences(user);
        BookCatalogResponse book = new BookCatalogResponse("OL1W", "Libro", List.of("Autora"),
                null, null, List.of(), "https://example.com/cover.jpg");
        when(catalog.resolve("BOOK", "OL1W", "access-token"))
                .thenReturn(CatalogResolution.available(book));
        when(catalog.resolve("SONG", "song-id", "access-token"))
                .thenReturn(CatalogResolution.unavailable());

        PreferencesResponse response = service.getOwnPreferences(user, "access-token");
        assertEquals(2, response.elements().size());
        assertEquals(book, response.elements().get(0).content());
        assertEquals(CatalogResolution.Status.AVAILABLE, response.elements().get(0).resolutionStatus());
        assertEquals(songId, response.elements().get(1).id());
        assertEquals(CatalogResolution.Status.UNAVAILABLE, response.elements().get(1).resolutionStatus());
        verify(catalog).resolve("BOOK", "OL1W", "access-token");
        verify(catalog).resolve("SONG", "song-id", "access-token");
    }
}
