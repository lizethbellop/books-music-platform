package com.musa.chat.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import com.musa.chat.exception.ConversationBusyException;
import org.springframework.dao.QueryTimeoutException;

import java.util.UUID;

@Repository
public class PrivateConversationLock {

    private final JdbcTemplate jdbcTemplate;

    public PrivateConversationLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(UUID firstUserId, UUID secondUserId) {
        String pairKey = buildPairKey(firstUserId, secondUserId);

        try {
            jdbcTemplate.execute(
                    """
                    SELECT pg_advisory_xact_lock(
                        hashtextextended(?, 0)
                    )
                    """,
                    (PreparedStatement statement) -> {
                        statement.setString(1, pairKey);
                        statement.setQueryTimeout(5);
                        statement.execute();
                        return null;
                    }
            );
        } catch (QueryTimeoutException exception) {
            throw new ConversationBusyException(exception);
        }
    }

    private String buildPairKey(UUID firstUserId, UUID secondUserId) {
        String first = firstUserId.toString();
        String second = secondUserId.toString();

        if (first.compareTo(second) > 0) {
            return "private-conversation:" + second + ":" + first;
        }

        return "private-conversation:" + first + ":" + second;
    }
}