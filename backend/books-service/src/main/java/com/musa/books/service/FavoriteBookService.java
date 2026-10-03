package com.musa.books.service;

import com.musa.books.entity.Book;
import com.musa.books.entity.FavoriteBook;
import com.musa.books.repository.FavoriteBookRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FavoriteBookService {

    private final FavoriteBookRepository favoriteBookRepository;
    private final BookService bookService;

    public FavoriteBookService(
            FavoriteBookRepository favoriteBookRepository,
            BookService bookService
    ) {
        this.favoriteBookRepository = favoriteBookRepository;
        this.bookService = bookService;
    }

    public FavoriteBook addFavorite(
            UUID userId,
            String externalId
    ) {

        Book book = bookService.getOrCreateBook(externalId);

        boolean alreadyFavorite =
                favoriteBookRepository.existsByUserIdAndBook_Id(
                        userId,
                        book.getId()
                );

        if (alreadyFavorite) {
            throw new IllegalStateException(
                    "El libro ya se encuentra en favoritos"
            );
        }

        FavoriteBook favoriteBook = FavoriteBook.builder()
                .userId(userId)
                .book(book)
                .build();

        return favoriteBookRepository.save(favoriteBook);
    }

    public List<FavoriteBook> getFavoritesByUser(UUID userId) {
        return favoriteBookRepository.findByUserId(userId);
    }
}