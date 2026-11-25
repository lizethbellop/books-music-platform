package com.musa.communities_service.polls.repository;

import com.musa.communities_service.polls.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PollVoteRepository
        extends JpaRepository<PollVote, UUID> {

    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO community_poll_votes (
                id,
                poll_id,
                option_id,
                user_id
            )
            VALUES (
                :voteId,
                :pollId,
                :optionId,
                :userId
            )
            ON CONFLICT ON CONSTRAINT uq_poll_vote_user
            DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("voteId") UUID voteId,
            @Param("pollId") UUID pollId,
            @Param("optionId") UUID optionId,
            @Param("userId") UUID userId
    );

    @Query(value = """
            SELECT
                o.id AS "optionId",
                o.label AS "label",
                o.position AS "position",
                COUNT(v.id) AS "voteCount"
            FROM community_poll_options o
            LEFT JOIN community_poll_votes v
                ON v.poll_id = o.poll_id
                AND v.option_id = o.id
            WHERE o.poll_id = :pollId
            GROUP BY o.id, o.label, o.position
            ORDER BY o.position
            """, nativeQuery = true)
    List<PollOptionResult> findResults(@Param("pollId") UUID pollId);
}