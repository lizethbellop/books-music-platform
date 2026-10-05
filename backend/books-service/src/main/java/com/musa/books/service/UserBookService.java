package com.musa.books.service;

import com.musa.books.entity.Book;
import com.musa.books.entity.UserBook;
import com.musa.books.enums.ReadingStatus;
import com.musa.books.repository.UserBookRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserBookService {

    private final UserBookRepository userBookRepository;
    private final BookService bookService;

    public UserBookService(
            UserBookRepository userBookRepository,
            BookService bookService
    ) {
        this.userBookRepository = userBookRepository;
        this.bookService = bookService;
    }

    public UserBook updateReadingStatus(
            UUID userId,
            String externalId,
            ReadingStatus readingStatus
    ) {

        Book book = bookService.getOrCreateBook(externalId);

        UserBook userBook = userBookRepository
                .findByUserIdAndBookId(
                        userId,
                        book.getId()
                )
                .orElseGet(() ->
                        UserBook.builder()
                                .userId(userId)
                                .book(book)
                                .build()
                );

        userBook.setReadingStatus(readingStatus);

        return userBookRepository.save(userBook);
    }

    public List<UserBook> getBooksByUser(UUID userId) {
        return userBookRepository.findByUserId(userId);
    }
}