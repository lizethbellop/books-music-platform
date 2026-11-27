package com.musa.social.repository;

import com.musa.social.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowedId(
        UUID followerId,
        UUID followedId
    );

    Optional<Follow> findByFollowerIdAndFollowedId(
        UUID followerId,
        UUID followedId
    );

    List<Follow> findByFollowerId(UUID followerId);

    List<Follow> findByFollowedId(UUID followedId);
}