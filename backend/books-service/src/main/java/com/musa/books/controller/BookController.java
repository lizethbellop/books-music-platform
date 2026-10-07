package com.musa.books.controller;

import com.musa.books.dto.BookDetailDto;
import com.musa.books.dto.BookSearchResultDto;
import com.musa.books.service.OpenLibraryService;
import com.musa.books.service.TranslationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.musa.books.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final OpenLibraryService openLibraryService;
    private final BookService bookService;

    public BookController(
            OpenLibraryService openLibraryService,
            BookService bookService
    ) {
        this.openLibraryService = openLibraryService;
        this.bookService = bookService;
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

        BookDetailDto detail =
                openLibraryService.getBookDetail(
                        externalId
                );

        BookDetailDto preparedDetail =
                bookService.prepareBookDetail(
                        detail
                );

        return ResponseEntity.ok(
                preparedDetail
        );
    }
}