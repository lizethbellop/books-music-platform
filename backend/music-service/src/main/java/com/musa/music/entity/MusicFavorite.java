package com.musa.music.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "music_favorite",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_music_favorite_user_content",
            columnNames = {"user_id", "music_content_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "music_content_id", nullable = false)
    private MusicContent musicContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}