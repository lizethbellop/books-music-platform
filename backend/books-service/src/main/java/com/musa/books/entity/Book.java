package com.musa.books.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String author;

    @Column(length = 30)
    private String isbn;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}