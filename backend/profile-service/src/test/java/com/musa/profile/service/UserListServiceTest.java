package com.musa.profile.service;

import com.musa.profile.dto.AddListElementRequest;
import com.musa.profile.dto.catalog.BookCatalogResponse;
import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.entity.ListElement;
import com.musa.profile.entity.Profile;
import com.musa.profile.entity.UserList;
import com.musa.profile.exception.ListElementAlreadyExistsException;
import com.musa.profile.exception.ListElementNotFoundException;
import com.musa.profile.exception.UserListNotFoundException;
import com.musa.profile.repository.ListElementRepository;
import com.musa.profile.repository.UserListRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private UserListRepository userListRepository;

    @Mock
    private ListElementRepository listElementRepository;

    @Mock
    private CatalogLookupService catalogLookupService;

    @InjectMocks
    private UserListService service;

    private UUID userId;
    private UUID listId;
    private Profile profile;
    private UserList list;
    private AddListElementRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        listId = UUID.randomUUID();

        profile = new Profile(userId, false);
        profile.setId(UUID.randomUUID());

        list = new UserList(
                profile.getId(),
                "Mi lista",
                "Descripción"
        );

        ReflectionTestUtils.setField(list, "id", listId);

        request = new AddListElementRequest("BOOK", "OL1W");
    }

    private void givenOwnedList() {
        when(profileService.getByUserId(userId))
                .thenReturn(profile);

        when(userListRepository.findById(listId))
                .thenReturn(Optional.of(list));
    }

    private CatalogResolution availableBook() {
        return CatalogResolution.available(
                new BookCatalogResponse(
                        "OL1W",
                        "Libro de prueba",
                        List.of("Autora"),
                        null,
                        null,
                        null,
                        "https://example.com/book.jpg"
                )
        );
    }

    @Test
    void debeValidarYGuardarReferenciaSinEspacios() {
        givenOwnedList();

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(availableBook());

        when(listElementRepository.saveAndFlush(any(ListElement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.addElement(
                userId,
                listId,
                new AddListElementRequest("BOOK", " OL1W "),
                "token-prueba"
        );

        assertEquals("OL1W", response.referenceId());

        var captor = ArgumentCaptor.forClass(ListElement.class);
        verify(listElementRepository).saveAndFlush(captor.capture());

        assertEquals(listId, captor.getValue().getListId());

        verify(catalogLookupService).resolve(
                "BOOK", "OL1W", "token-prueba"
        );
    }

    @Test
    void noDebeGuardarDuplicadosNiConsultarCatalogo() {
        givenOwnedList();

        when(listElementRepository
                .existsByListIdAndElementTypeAndReferenceId(
                        listId, "BOOK", "OL1W"
                ))
                .thenReturn(true);

        assertThrows(
                ListElementAlreadyExistsException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );

        verifyNoInteractions(catalogLookupService);
        verify(listElementRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void noDebeGuardarContenidoInexistente() {
        givenOwnedList();

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(CatalogResolution.notFound());

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );

        assertEquals(404, error.getStatusCode().value());

        verify(listElementRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void noDebeGuardarCuandoElCatalogoEstaCaido() {
        givenOwnedList();

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(CatalogResolution.unavailable());

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );

        assertEquals(503, error.getStatusCode().value());

        verify(listElementRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void noDebeAgregarContenidoAListaAjena() {
        UserList otherList = new UserList(
                UUID.randomUUID(),
                "Lista ajena",
                null
        );

        when(profileService.getByUserId(userId))
                .thenReturn(profile);

        when(userListRepository.findById(listId))
                .thenReturn(Optional.of(otherList));

        assertThrows(
                UserListNotFoundException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );

        verifyNoInteractions(
                catalogLookupService,
                listElementRepository
        );
    }

    @Test
    void debeConservarLaListaSiUnContenidoNoPuedeCargarse() {
        givenOwnedList();

        var book = new ListElement(listId, "BOOK", "OL1W");
        var song = new ListElement(listId, "SONG", "song-test");

        when(listElementRepository.findByListId(listId))
                .thenReturn(List.of(book, song));

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(availableBook());

        when(catalogLookupService.resolve(
                "SONG", "song-test", "token-prueba"
        )).thenReturn(CatalogResolution.unavailable());

        var detail = service.getListDetail(
                userId, listId, "token-prueba"
        );

        assertAll(
                () -> assertEquals(2, detail.elements().size()),
                () -> assertEquals(
                        CatalogResolution.Status.AVAILABLE,
                        detail.elements().get(0).resolutionStatus()
                ),
                () -> assertEquals(
                        CatalogResolution.Status.UNAVAILABLE,
                        detail.elements().get(1).resolutionStatus()
                ),
                () -> assertNull(
                        detail.elements().get(1).content()
                )
        );
    }

    @Test
    void debeQuitarElementoDeLaListaPropia() {
        givenOwnedList();

        UUID elementId = UUID.randomUUID();
        var element = new ListElement(listId, "BOOK", "OL1W");

        when(listElementRepository.findById(elementId))
                .thenReturn(Optional.of(element));

        service.removeElement(userId, listId, elementId);

        verify(listElementRepository).delete(element);
        verifyNoInteractions(catalogLookupService);
    }

    @Test
    void noDebeQuitarElementoDeOtraLista() {
        givenOwnedList();

        UUID elementId = UUID.randomUUID();
        var otherElement = new ListElement(
                UUID.randomUUID(),
                "BOOK",
                "OL1W"
        );

        when(listElementRepository.findById(elementId))
                .thenReturn(Optional.of(otherElement));

        assertThrows(
                ListElementNotFoundException.class,
                () -> service.removeElement(
                        userId, listId, elementId
                )
        );

        verify(listElementRepository, never()).delete(any());
    }

    @Test
    void debeConvertirConflictoDeDuplicadoEnErrorConocido() {
        givenOwnedList();

        when(listElementRepository
                .existsByListIdAndElementTypeAndReferenceId(
                        listId, "BOOK", "OL1W"
                ))
                .thenReturn(false, true);

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(availableBook());

        when(listElementRepository.saveAndFlush(any(ListElement.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Conflicto simulado"
                ));

        assertThrows(
                ListElementAlreadyExistsException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );
    }

    @Test
    void noDebeConfundirOtroErrorDeBdConUnDuplicado() {
        givenOwnedList();

        when(catalogLookupService.resolve(
                "BOOK", "OL1W", "token-prueba"
        )).thenReturn(availableBook());

        var failure = new DataIntegrityViolationException(
                "Otro fallo simulado"
        );

        when(listElementRepository.saveAndFlush(any(ListElement.class)))
                .thenThrow(failure);

        var result = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.addElement(
                        userId, listId, request, "token-prueba"
                )
        );

        assertSame(failure, result);
    }
}