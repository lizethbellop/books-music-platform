package com.musa.communities_service.polls.repository;

import com.musa.communities_service.polls.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollOptionRepository
        extends JpaRepository<PollOption, UUID> {

    List<PollOption> findByPollIdOrderByPositionAsc(UUID pollId);

    boolean existsByIdAndPollId(UUID optionId, UUID pollId);
}