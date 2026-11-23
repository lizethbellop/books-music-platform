package com.musa.communities_service.polls.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PollChanges {

    private final JdbcTemplate jdbc;

    public PollChanges(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID pollId) {
        jdbc.update("""
                INSERT INTO community_poll_changes (poll_id)
                VALUES (?)
                ON CONFLICT (poll_id)
                DO UPDATE SET
                    changed_at = clock_timestamp(),
                    revision = gen_random_uuid()
                """,
                pollId
        );
    }
}