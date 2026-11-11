package com.musa.profile.service;

import com.musa.profile.dto.*;
import com.musa.profile.entity.Profile;
import com.musa.profile.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:initial-preferences;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ProfileService.class, PreferenceService.class, UserListService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InitialPreferencesTest {
    @Autowired ProfileRepository profiles;
    @Autowired PreferenceService preferences;
    @Autowired UserListService lists;
    @MockitoBean CatalogLookupService catalog;
    UUID user;
    @BeforeEach void createProfile() {
        user = UUID.randomUUID();
        profiles.saveAndFlush(new Profile(user, false));
    }

    @Test void storesBothMediaTypesAndRetryDoesNotDuplicateOrLeakToAnotherUser() {
        var request = new SaveInitialPreferencesRequest(List.of(
                new AddPreferenceElementRequest("BOOK", "OL1W"),
                new AddPreferenceElementRequest("SONG", "spotify-track"),
                new AddPreferenceElementRequest("BOOK", "OL1W")));
        assertEquals(2, preferences.saveInitialPreferences(user, request).elements().size());
        assertEquals(2, preferences.saveInitialPreferences(user, request).elements().size());
        // New preferences immediately become book recommendation seeds, without custom lists.
        assertEquals(List.of("OL1W"), lists.getOwnBookSeeds(user));
        UUID other = UUID.randomUUID();
        profiles.saveAndFlush(new Profile(other, false));
        assertTrue(preferences.getOwnPreferences(other).elements().isEmpty());
        assertTrue(lists.getOwnBookSeeds(other).isEmpty());
        verifyNoInteractions(catalog);
    }

    @Test void validationFailureRollsBackTheWholeBatchAndKeepsEarlierPreferences() {
        preferences.addElement(user, new AddPreferenceElementRequest("BOOK", "OL1W"));
        assertThrows(ResponseStatusException.class, () -> preferences.saveInitialPreferences(user,
                new SaveInitialPreferencesRequest(List.of(
                        new AddPreferenceElementRequest("BOOK", "OL2W"),
                        new AddPreferenceElementRequest("BOOK", "not-a-work")))));
        var stored = preferences.getOwnPreferences(user).elements();
        assertEquals(1, stored.size());
        assertEquals("OL1W", stored.getFirst().referenceId());
    }
}
