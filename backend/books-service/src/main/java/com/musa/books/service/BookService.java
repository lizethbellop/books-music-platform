package com.musa.books.service;

import com.musa.books.dto.BookDetailDto;
import com.musa.books.entity.Book;
import com.musa.books.exception.ResourceNotFoundException;
import com.musa.books.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final OpenLibraryService openLibraryService;
    private final TranslationService translationService;
    private final ConcurrentHashMap<String, Object> bookLocks = new ConcurrentHashMap<>();

    public BookService(
            BookRepository bookRepository,
            OpenLibraryService openLibraryService,
            TranslationService translationService
    ) {
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
        this.translationService = translationService;
    }

    public Book getBookById(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "El libro no existe"
                        )
                );
    }

    public Optional<Book> getBookByExternalId(
            String externalId
    ) {
        return bookRepository.findByExternalId(
                externalId
        );
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public Book getOrCreateBook(String externalId) {

        Object lock = bookLocks.computeIfAbsent(
                externalId,
                key -> new Object()
        );

        synchronized (lock) {

            Optional<Book> existingBook =
                    bookRepository.findByExternalId(
                            externalId
                    );

            if (existingBook.isPresent()) {
                return existingBook.get();
            }

            BookDetailDto detail =
                    openLibraryService.getBookDetail(
                            externalId
                    );

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
                    .descriptionOriginal(
                            detail.getDescription()
                    )
                    .descriptionEs(null)
                    .build();

            return bookRepository.save(book);
        }
    }

    public BookDetailDto prepareBookDetail(
            BookDetailDto detail
    ) {

        String externalId =
                detail.getExternalId();

        Object lock = bookLocks.computeIfAbsent(
                externalId,
                key -> new Object()
        );

        synchronized (lock) {

            /*
            * MUY IMPORTANTE:
            * volvemos a consultar dentro del lock.
            *
            * Así, si otra petición acaba de crear
            * el libro, aquí ya lo encontraremos.
            */
            Optional<Book> existingBook =
                    bookRepository.findByExternalId(
                            externalId
                    );

            Book book;

            if (existingBook.isPresent()) {

                book = existingBook.get();

            } else {

                String authors = null;

                if (detail.getAuthors() != null
                        && !detail.getAuthors().isEmpty()) {

                    authors = String.join(
                            ", ",
                            detail.getAuthors()
                    );
                }

                book = Book.builder()
                        .externalId(externalId)
                        .title(detail.getTitle())
                        .author(authors)
                        .coverUrl(detail.getCoverUrl())
                        .build();
            }

            String originalDescription =
                    detail.getDescription();

            String spanishDescription =
                    book.getDescriptionEs();

            /*
            * Guardamos la descripción original
            * si todavía no existe.
            */
            if (originalDescription != null
                    && !originalDescription.isBlank()
                    && (book.getDescriptionOriginal() == null
                        || book.getDescriptionOriginal().isBlank())) {

                book.setDescriptionOriginal(
                        originalDescription
                );
            }

            /*
            * Solo traducimos cuando todavía
            * no existe traducción en PostgreSQL.
            */
            if (spanishDescription == null
                    || spanishDescription.isBlank()) {

                if (originalDescription != null
                        && !originalDescription.isBlank()) {

                    try {

                        spanishDescription =
                                translationService
                                        .translateToSpanish(
                                                originalDescription
                                        );

                        book.setDescriptionEs(
                                spanishDescription
                        );

                    } catch (Exception e) {

                        /*
                        * DeepL nunca debe impedir
                        * consultar el libro.
                        */
                        spanishDescription =
                                originalDescription;
                    }
                }
            }

            /*
            * Como estamos dentro del lock del externalId,
            * ya no puede haber dos INSERT simultáneos
            * para el mismo libro dentro de esta instancia.
            */
            bookRepository.save(book);

            String finalDescription =
                    spanishDescription != null
                            && !spanishDescription.isBlank()
                            ? spanishDescription
                            : originalDescription;

            return new BookDetailDto(
                    detail.getExternalId(),
                    detail.getTitle(),
                    detail.getAuthors(),
                    finalDescription,
                    detail.getFirstPublishDate(),
                    detail.getSubjects(),
                    detail.getCoverUrl()
            );
        }
    }
}