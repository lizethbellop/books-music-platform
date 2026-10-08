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
import com.musa.profile.dto.ResolvedListElementResponse;
import org.springframework.transaction.annotation.Propagation;
import com.musa.profile.dto.catalog.CatalogResolution;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserListService {

    private final ProfileService profileService;
    private final UserListRepository userListRepository;
    private final ListElementRepository listElementRepository;
    private final CatalogLookupService catalogLookupService;

    public UserListService(
            ProfileService profileService,
            UserListRepository userListRepository,
            ListElementRepository listElementRepository,
            CatalogLookupService catalogLookupService
    ) {
        this.profileService = profileService;
        this.userListRepository = userListRepository;
        this.listElementRepository = listElementRepository;
        this.catalogLookupService = catalogLookupService;
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ListElementResponse addElement(
            UUID userId,
            UUID listId,
            AddListElementRequest request,
            String accessToken
    ) {
        UserList list = getOwnedList(userId, listId);

        String elementType = request.elementType();
        String referenceId = request.referenceId().trim();

        boolean alreadyExists =
                listElementRepository.existsByListIdAndElementTypeAndReferenceId(
                        list.getId(),
                        elementType,
                        referenceId
                );

        if (alreadyExists) {
            throw new ListElementAlreadyExistsException();
        }

        CatalogResolution resolution = catalogLookupService.resolve(
                elementType,
                referenceId,
                accessToken
        );

        if (resolution.resolutionStatus()
                == CatalogResolution.Status.NOT_FOUND) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El contenido solicitado no existe."
            );
        }

        if (resolution.resolutionStatus()
                != CatalogResolution.Status.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el contenido. Intenta nuevamente."
            );
        }

        try {
            ListElement element = listElementRepository.saveAndFlush(
                    new ListElement(
                            list.getId(),
                            elementType,
                            referenceId
                    )
            );

            return ListElementResponse.from(element);
        } catch (DataIntegrityViolationException exception) {
            boolean duplicate =
                    listElementRepository.existsByListIdAndElementTypeAndReferenceId(
                            list.getId(),
                            elementType,
                            referenceId
                    );

            if (duplicate) {
                throw new ListElementAlreadyExistsException();
            }

            throw exception;
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserListDetailResponse getListDetail(
            UUID userId,
            UUID listId,
            String accessToken
    ) {
        UserList list = getOwnedList(userId, listId);

        List<ResolvedListElementResponse> elements =
                listElementRepository.findByListId(list.getId())
                        .stream()
                        .map(element -> ResolvedListElementResponse.from(
                                element,
                                catalogLookupService.resolve(
                                        element.getElementType(),
                                        element.getReferenceId(),
                                        accessToken
                                )
                        ))
                        .toList();

        return UserListDetailResponse.from(list, elements);
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