package com.musa.music;

import com.musa.music.entity.*;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
class MusicServiceApplicationTests {
    @Autowired EntityManager entityManager;

    @Test void contextLoads() {}

    @Test
    @Transactional
    void persistsUuidUsersAndKeepsTheirActionsSeparate() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        MusicContent content = MusicContent.builder()
            .spotifyId("uuid-test-" + UUID.randomUUID())
            .contentType(MusicContentType.SONG).name("UUID integration test").build();
        entityManager.persist(content);
        for (UUID user : new UUID[]{first, second}) {
            entityManager.persist(MusicFavorite.builder().userId(user).musicContent(content).build());
            entityManager.persist(MusicRating.builder().userId(user).musicContent(content).rating(3.5).build());
            entityManager.persist(MusicReview.builder().userId(user).musicContent(content).reviewText("test").build());
        }
        entityManager.flush();
        entityManager.clear();
        for (Class<?> type : new Class<?>[]{MusicFavorite.class, MusicRating.class, MusicReview.class}) {
            for (UUID user : new UUID[]{first, second}) {
                var rows = entityManager.createQuery("select e from " + type.getSimpleName()
                    + " e where e.userId = :user and e.musicContent.id = :content", type)
                    .setParameter("user", user).setParameter("content", content.getId()).getResultList();
                assertEquals(1, rows.size());
                Object row = rows.get(0);
                UUID stored = row instanceof MusicFavorite f ? f.getUserId()
                    : row instanceof MusicRating r ? r.getUserId() : ((MusicReview) row).getUserId();
                assertEquals(user, stored);
            }
        }
        // Spring rolls back every test record, including the temporary content.
    }
}
