package com.musa.communities_service.polls.repository;

import java.util.UUID;

public interface PollOptionResult {

    UUID getOptionId();

    String getLabel();

    Short getPosition();

    Long getVoteCount();
}