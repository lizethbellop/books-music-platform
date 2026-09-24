package com.musa.profile.service;

import com.musa.profile.dto.CreateUserListRequest;
import com.musa.profile.dto.UserListResponse;
import com.musa.profile.entity.Profile;
import com.musa.profile.entity.UserList;
import com.musa.profile.repository.UserListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.musa.profile.exception.UserListNotFoundException;
import com.musa.profile.dto.UpdateUserListRequest;
import com.musa.profile.dto.AddListElementRequest;
import com.musa.profile.dto.ListElementResponse;
import com.musa.profile.entity.ListElement;
import com.musa.profile.exception.ListElementAlreadyExistsException;
import com.musa.profile.repository.ListElementRepository;
import com.musa.profile.dto.UserListDetailResponse;
import com.musa.profile.exception.ListElementNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserListService {

    private final ProfileService profileService;
    private final UserListRepository userListRepository;
    private final ListElementRepository listElementRepository;

    public UserListService(
            ProfileService profileService,
            UserListRepository userListRepository,
            ListElementRepository listElementRepository
    ) {
        this.profileService = profileService;
        this.userListRepository = userListRepository;
        this.listElementRepository = listElementRepository;
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

    private UserList getOwnedList(UUID userId, UUID listId){
        Profile profile = profileService.getByUserId(userId);

        return userListRepository.findById(listId).filter(list ->list.getProfileId().equals(profile.getId()))
                .orElseThrow(() -> new UserListNotFoundException(listId));
    }

    @Transactional
    public UserListResponse updateList(UUID userId, UUID listId, UpdateUserListRequest request){

        UserList list = getOwnedList(userId, listId);

        list.setName(request.name().trim());
        list.setDescription(request.description());

        return UserListResponse.from(list);
    }

    @Transactional
    public ListElementResponse addElement(UUID userId, UUID listId, AddListElementRequest request){
        UserList list = getOwnedList(userId, listId);
        boolean alreadyExists = listElementRepository.existsByListIdAndElementTypeAndReferenceId(list.getId(), request.elementType(), request.referenceId());

        if(alreadyExists){
            throw new ListElementAlreadyExistsException();
        }

        ListElement element = listElementRepository.save(new ListElement(list.getId(), request.elementType(), request.referenceId()));

        return ListElementResponse.from(element);
    }

    public UserListDetailResponse getListDetail(UUID userId, UUID listId) {
        UserList list = getOwnedList(userId, listId);

        return UserListDetailResponse.from(
                list,
                listElementRepository.findByListId(list.getId())
        );
    }

    @Transactional
    public void removeElement(UUID userId, UUID listId, UUID elementId) {
        UserList list = getOwnedList(userId, listId);

        ListElement element = listElementRepository.findById(elementId)
                .orElseThrow(() -> new ListElementNotFoundException(elementId));

        if (!element.getListId().equals(list.getId())) {
            throw new ListElementNotFoundException(elementId);
        }

        listElementRepository.delete(element);
    }

    @Transactional
    public void deleteList(UUID userId, UUID listId) {
        UserList list = getOwnedList(userId, listId);
        userListRepository.delete(list);
    }

}