package com.musa.profile.repository;

import com.musa.profile.entity.ListElement;
import com.musa.profile.entity.UserList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:book-seeds;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookSeedRepositoryTest {
    @Autowired ListElementRepository elements;
    @Autowired UserListRepository lists;

    @Test void excludesOtherProfilesAndMusicAndDeduplicatesAcrossLists() {
        UUID owner = UUID.randomUUID();
        var first = lists.saveAndFlush(new UserList(owner, "Uno", null));
        var second = lists.saveAndFlush(new UserList(owner, "Dos", null));
        var other = lists.saveAndFlush(new UserList(UUID.randomUUID(), "Ajena", null));
        elements.saveAllAndFlush(List.of(
            new ListElement(first.getId(), "BOOK", "OL1W"),
            new ListElement(second.getId(), "BOOK", "OL1W"),
            new ListElement(second.getId(), "BOOK", "OL2W"),
            new ListElement(first.getId(), "SONG", "spotify-id"),
            new ListElement(other.getId(), "BOOK", "OL99W")
        ));
        assertEquals(List.of("OL1W", "OL2W"), elements.findBookReferencesByProfileId(owner));
        assertEquals(List.of(), elements.findBookReferencesByProfileId(UUID.randomUUID()));
    }
}
