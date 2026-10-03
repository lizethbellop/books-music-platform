package com.musa.books.controller;

import com.musa.books.dto.FavoriteBookResponse;
import com.musa.books.entity.FavoriteBook;
import com.musa.books.service.FavoriteBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
            @RequestParam UUID userId
    ) {
        FavoriteBook favorite = favoriteBookService.addFavorite(userId, externalId);

        return ResponseEntity.ok(toResponse(favorite));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<FavoriteBookResponse>> getFavoritesByUser(
            @RequestParam UUID userId
    ) {
        List<FavoriteBookResponse> favorites =
                favoriteBookService.getFavoritesByUser(userId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(favorites);
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