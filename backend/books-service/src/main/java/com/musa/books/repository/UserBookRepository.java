package com.musa.books.repository;

import com.musa.books.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBookRepository extends JpaRepository<UserBook, UUID> {

    Optional<UserBook> findByUserIdAndBookId(UUID userId, UUID bookId);

    List<UserBook> findByUserId(UUID userId);

    List<UserBook> findByBookId(UUID bookId);
}