package com.musa.users.controller;
import com.musa.users.entity.User;
import com.musa.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ChatUserDirectoryTest {
    @Test void searchesOnlyActiveUsersAndReturnsPublicFields() {
        var repository = mock(UserRepository.class);
        var user = new User(); user.setId(UUID.randomUUID()); user.setUsername("ana");
        when(repository.findByIsActiveTrueAndUsernameStartingWithIgnoreCaseOrderByUsernameAsc(eq("an"), any(Pageable.class)))
                .thenReturn(List.of(user));
        var result = new UsernamesController(repository).search(" an ");
        assertEquals(List.of(new UsernamesController.UserSummary(user.getId(), "ana")), result);
        verify(repository).findByIsActiveTrueAndUsernameStartingWithIgnoreCaseOrderByUsernameAsc(eq("an"),
                argThat(page -> page.getPageSize() == 20 && page.getPageNumber() == 0));
    }
    @Test void invalidSearchDoesNotQueryDatabase() {
        var repository = mock(UserRepository.class); var controller = new UsernamesController(repository);
        assertThrows(ResponseStatusException.class, () -> controller.search("a"));
        assertThrows(ResponseStatusException.class, () -> controller.search("x".repeat(41)));
        verifyNoInteractions(repository);
    }
    @Test void repositoryMethodResolvesExistingUserFields() {
        assertDoesNotThrow(() -> new PartTree(
                "findByIsActiveTrueAndUsernameStartingWithIgnoreCaseOrderByUsernameAsc", User.class));
    }
}
