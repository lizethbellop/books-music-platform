package com.musa.users.controller;

import com.musa.users.repository.UserRepository;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/users")
public class UsernamesController {
    private final UserRepository users;
    public UsernamesController(UserRepository users) { this.users = users; }

    public record UserSummary(UUID id, String username) {}

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<UserSummary> search(@RequestParam("q") String query) {
        String prefix = query.trim();
        if (prefix.length() < 2 || prefix.length() > 40) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Busca con entre 2 y 40 caracteres.");
        }
        return users.findByIsActiveTrueAndUsernameStartingWithIgnoreCaseOrderByUsernameAsc(
                prefix, org.springframework.data.domain.PageRequest.of(0, 20)).stream()
                .map(user -> new UserSummary(user.getId(), user.getUsername())).toList();
    }

    // Información pública mínima; SecurityConfig requiere sesión en esta ruta.
    @GetMapping("/usernames")
    @Transactional(readOnly = true)
    public Map<UUID, String> usernames(@RequestParam("ids") @Size(min = 1, max = 100) List<UUID> ids) {
        Map<UUID, String> result = new LinkedHashMap<>();
        for (var user : users.findAllById(ids.stream().distinct().toList())) {
            if (Boolean.TRUE.equals(user.getIsActive())) result.put(user.getId(), user.getUsername());
        }
        return result;
    }
}
