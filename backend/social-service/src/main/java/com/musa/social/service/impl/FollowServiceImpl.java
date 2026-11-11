package com.musa.social.service.impl;

import com.musa.social.entity.Follow;
import com.musa.social.exception.BadRequestException;
import com.musa.social.exception.ConflictException;
import com.musa.social.exception.ResourceNotFoundException;
import com.musa.social.repository.FollowRepository;
import com.musa.social.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;

    @Override
    @Transactional
    public Follow followUser(UUID followerId, UUID followedId) {

        if (followerId.equals(followedId)) {
            throw new BadRequestException(
                "Un usuario no puede seguirse a sí mismo"
            );
        }

        if (followRepository.existsByFollowerIdAndFollowedId(
                followerId,
                followedId
        )) {
            throw new ConflictException(
                "El usuario ya sigue a este usuario"
            );
        }

        Follow follow = Follow.builder()
            .followerId(followerId)
            .followedId(followedId)
            .build();

        return followRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollowUser(UUID followerId, UUID followedId) {

        Follow follow = followRepository
            .findByFollowerIdAndFollowedId(
                followerId,
                followedId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "La relación de seguimiento no existe"
                )
            );

        followRepository.delete(follow);
    }

    @Override
    @Transactional
    public void removeFollower(UUID userId, UUID followerId) {

        Follow follow = followRepository
            .findByFollowerIdAndFollowedId(
                followerId,
                userId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Este usuario no te sigue"
                )
            );

        followRepository.delete(follow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowers(UUID userId) {
        return followRepository.findByFollowedId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowing(UUID userId) {
        return followRepository.findByFollowerId(userId);
    }
}