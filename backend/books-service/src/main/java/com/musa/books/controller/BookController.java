package com.musa.books.controller;

import com.musa.books.dto.BookDetailDto;
import com.musa.books.dto.BookSearchResultDto;
import com.musa.books.service.OpenLibraryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final OpenLibraryService openLibraryService;

    public BookController(OpenLibraryService openLibraryService) {
        this.openLibraryService = openLibraryService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookSearchResultDto>> searchBooks(
            @RequestParam String q
    ) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<BookSearchResultDto> books =
                openLibraryService.searchBooks(q);

        return ResponseEntity.ok(books);
    }

    @GetMapping("/explore")
    public ResponseEntity<List<BookSearchResultDto>> getExploreBooks() {

        List<BookSearchResultDto> books =
                openLibraryService.getExploreBooks();

        return ResponseEntity.ok(books);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<BookDetailDto> getBookDetail(
            @PathVariable String externalId
    ) {

        BookDetailDto book =
                openLibraryService.getBookDetail(externalId);

        return ResponseEntity.ok(book);
    }
}