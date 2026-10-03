package com.musa.books.repository;

import com.musa.books.entity.FavoriteBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteBookRepository extends JpaRepository<FavoriteBook, UUID> {

    Optional<FavoriteBook> findByUserIdAndBook_Id(UUID userId, UUID bookId);

    List<FavoriteBook> findByUserId(UUID userId);

    boolean existsByUserIdAndBook_Id(UUID userId, UUID bookId);
}
