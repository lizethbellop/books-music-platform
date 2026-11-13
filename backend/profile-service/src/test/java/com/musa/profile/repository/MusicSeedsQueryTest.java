package com.musa.profile.repository;

import com.musa.profile.controller.MusicRecommendationSeedsController;
import com.musa.profile.entity.*;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:music-seeds;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(MusicRecommendationSeedsController.class)
class MusicSeedsQueryTest {
    @Autowired EntityManager em;
    @Autowired MusicRecommendationSeedsController controller;

    @Test void queriesRealRelationshipsDeduplicatesAlbumsAndExcludesOtherUsersAndBooks() {
        UUID owner = UUID.randomUUID();
        var profile = new Profile(owner, false);
        var otherProfile = new Profile(UUID.randomUUID(), false);
        em.persist(profile); em.persist(otherProfile); em.flush();
        var list = new UserList(profile.getId(), "Lista", null);
        var otherList = new UserList(otherProfile.getId(), "Ajena", null);
        var preference = new Preference(profile.getId());
        em.persist(list); em.persist(otherList); em.persist(preference); em.flush();
        em.persist(new ListElement(list.getId(), "ALBUM", "album-one"));
        em.persist(new ListElement(list.getId(), "BOOK", "OL1W"));
        em.persist(new ListElement(otherList.getId(), "SONG", "private-song"));
        em.persist(new PreferenceElement(preference.getId(), "ALBUM", "album-one"));
        em.persist(new PreferenceElement(preference.getId(), "ARTIST", "artist-one"));
        em.flush();
        Jwt jwt = Jwt.withTokenValue("fixture").header("alg", "none").subject(owner.toString()).build();
        var refs = controller.getOwnSeeds(jwt).getBody().items();
        assertEquals(2, refs.size());
        assertTrue(refs.contains(new MusicRecommendationSeedsController.Ref("ALBUM", "album-one")));
        assertTrue(refs.contains(new MusicRecommendationSeedsController.Ref("ARTIST", "artist-one")));
        Jwt absent = Jwt.withTokenValue("fixture").header("alg", "none").subject(UUID.randomUUID().toString()).build();
        assertEquals(List.of(), controller.getOwnSeeds(absent).getBody().items());
    }
}
