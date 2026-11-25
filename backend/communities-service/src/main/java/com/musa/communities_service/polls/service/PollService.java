package com.musa.communities_service.polls.service;

import com.musa.communities_service.polls.dto.CreatePollRequest;
import com.musa.communities_service.polls.dto.PollResponse;
import com.musa.communities_service.polls.dto.PollResultsResponse;
import com.musa.communities_service.polls.dto.VoteRequest;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface PollService {

    PollResponse create(
            UUID communityId,
            UUID currentUserId,
            CreatePollRequest request
    );

    Slice<PollResponse> list(
            UUID communityId,
            UUID currentUserId,
            int page,
            int size
    );

    PollResponse get(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    );

    PollResultsResponse results(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    );

    PollResultsResponse vote(
            UUID communityId,
            UUID pollId,
            UUID currentUserId,
            VoteRequest request
    );

    PollResponse close(
            UUID communityId,
            UUID pollId,
            UUID currentUserId
    );
}