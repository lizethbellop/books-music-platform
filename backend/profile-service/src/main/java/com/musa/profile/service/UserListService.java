package com.musa.profile.service;

import com.musa.profile.dto.CreateUserListRequest;
import com.musa.profile.dto.UserListResponse;
import com.musa.profile.entity.Profile;
import com.musa.profile.entity.UserList;
import com.musa.profile.repository.UserListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserListService {

    private final ProfileService profileService;
    private final UserListRepository userListRepository;

    public UserListService(
            ProfileService profileService,
            UserListRepository userListRepository
    ) {
        this.profileService = profileService;
        this.userListRepository = userListRepository;
    }

    @Transactional
    public UserListResponse createList(
            UUID userId,
            CreateUserListRequest request
    ) {
        Profile profile = profileService.getByUserId(userId);

        UserList list = new UserList(
                profile.getId(),
                request.name().trim(),
                request.description()
        );

        return UserListResponse.from(userListRepository.save(list));
    }

    public List<UserListResponse> getOwnLists(UUID userId) {
        Profile profile = profileService.getByUserId(userId);

        return userListRepository
                .findByProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(UserListResponse::from)
                .toList();
    }
}