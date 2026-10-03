package com.musa.books.service;

import com.musa.books.dto.BookDetailDto;
import com.musa.books.entity.Book;
import com.musa.books.exception.ResourceNotFoundException;
import com.musa.books.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final OpenLibraryService openLibraryService;

    public BookService(
            BookRepository bookRepository,
            OpenLibraryService openLibraryService
    ) {
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
    }

    public Book getBookById(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() ->
                    new ResourceNotFoundException("El libro no existe")
                );
    }

    public Optional<Book> getBookByExternalId(String externalId) {
        return bookRepository.findByExternalId(externalId);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public Book getOrCreateBook(String externalId) {

        Optional<Book> existingBook =
                bookRepository.findByExternalId(externalId);

        if (existingBook.isPresent()) {
            return existingBook.get();
        }

        BookDetailDto detail =
                openLibraryService.getBookDetail(externalId);

        String authors = null;

        if (detail.getAuthors() != null
                && !detail.getAuthors().isEmpty()) {

            authors = String.join(
                    ", ",
                    detail.getAuthors()
            );
        }

        Book book = Book.builder()
                .externalId(externalId)
                .title(detail.getTitle())
                .author(authors)
                .coverUrl(detail.getCoverUrl())
                .build();

        return bookRepository.save(book);
    }
}