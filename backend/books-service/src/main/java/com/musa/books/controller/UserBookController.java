package com.musa.books.controller;

import com.musa.books.dto.ReadingStatusRequest;
import com.musa.books.dto.UserBookResponse;
import com.musa.books.entity.UserBook;
import com.musa.books.service.UserBookService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/books")
public class UserBookController {

    private final UserBookService userBookService;

    public UserBookController(UserBookService userBookService) {
        this.userBookService = userBookService;
    }

    @PutMapping("/{externalId}/reading-status")
    public ResponseEntity<UserBookResponse> updateReadingStatus(
            @PathVariable String externalId,
            @RequestParam UUID userId,
            @Valid @RequestBody ReadingStatusRequest request
    ) {

        UserBook userBook =
                userBookService.updateReadingStatus(
                        userId,
                        externalId,
                        request.getStatus()
                );

        UserBookResponse response =
                new UserBookResponse(
                        userBook.getBook().getExternalId(),
                        userBook.getBook().getTitle(),
                        userBook.getBook().getAuthor(),
                        userBook.getBook().getCoverUrl(),
                        userBook.getReadingStatus()
                );

        return ResponseEntity.ok(response);
    }
}