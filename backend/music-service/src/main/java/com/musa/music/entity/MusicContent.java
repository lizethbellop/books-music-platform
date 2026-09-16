package com.musa.music.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "music_content",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_music_content_spotify_type",
            columnNames = {"spotify_id", "content_type"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_id", nullable = false)
    private String spotifyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private MusicContentType contentType;

    @Column(nullable = false)
    private String name;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}