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
