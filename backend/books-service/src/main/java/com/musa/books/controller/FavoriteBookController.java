package com.musa.books.controller;

import com.musa.books.dto.FavoriteBookResponse;
import com.musa.books.entity.FavoriteBook;
import com.musa.books.service.FavoriteBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.musa.books.config.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/books")
public class FavoriteBookController {

    private final FavoriteBookService favoriteBookService;

    public FavoriteBookController(FavoriteBookService favoriteBookService) {
        this.favoriteBookService = favoriteBookService;
    }

    @PostMapping("/{externalId}/favorites")
    public ResponseEntity<FavoriteBookResponse> addFavorite(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        FavoriteBook favorite = favoriteBookService.addFavorite(userId, externalId);

        return ResponseEntity.ok(toResponse(favorite));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<FavoriteBookResponse>> getFavoritesByUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        List<FavoriteBookResponse> favorites =
                favoriteBookService.getFavoritesByUser(userId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(favorites);
    }

    @DeleteMapping("/{externalId}/favorites")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        favoriteBookService.removeFavorite(userId, externalId);

        return ResponseEntity.noContent().build();
    }

    private FavoriteBookResponse toResponse(FavoriteBook favorite) {
        return new FavoriteBookResponse(
                favorite.getBook().getExternalId(),
                favorite.getBook().getTitle(),
                favorite.getBook().getAuthor(),
                favorite.getBook().getCoverUrl()
        );
    }
}