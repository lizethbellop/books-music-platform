package com.musa.books.repository;

import com.musa.books.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByUserIdAndBook_Id(UUID userId, UUID bookId);

    List<Review> findByBook_Id(UUID bookId);

    List<Review> findByUserId(UUID userId);
}