package com.musa.books.repository;

import com.musa.books.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByUserIdAndBook_Id(UUID userId, UUID bookId);

    List<Review> findByBook_Id(UUID bookId);

    List<Review> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.userId = :userId
            """)
    BigDecimal findAverageRatingByUserId(
            @Param("userId") UUID userId
    );
}