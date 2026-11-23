package com.musa.communities_service.polls.repository;

import com.musa.communities_service.polls.entity.Poll;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {

    Slice<Poll> findByCommunityIdOrderByCreatedAtDescIdDesc(
            UUID communityId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Poll p WHERE p.id = :pollId")
    Optional<Poll> findForVoting(@Param("pollId") UUID pollId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Poll p WHERE p.id = :pollId")
    Optional<Poll> findForClosing(@Param("pollId") UUID pollId);
}