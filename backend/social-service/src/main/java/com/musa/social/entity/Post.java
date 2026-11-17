package com.musa.social.entity;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // AUTOR

    @Column(name = "author_id", nullable = false)
    private UUID authorId;


    // CONTENIDO

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;


    @Enumerated(EnumType.STRING)
    @Column(
        name = "post_origin",
        nullable = false,
        length = 20
    )
    @Builder.Default
    private PostOrigin postOrigin = PostOrigin.USER_CREATED;


    @Enumerated(EnumType.STRING)
    @Column(
        name = "activity_type",
        length = 50
    )
    private ActivityType activityType;


    @Enumerated(EnumType.STRING)
    @Column(
        name = "content_type",
        length = 20
    )
    private ContentType contentType;


    @Column(
        name = "reference_id",
        length = 255
    )
    private String referenceId;


    // PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "activity_data",
        columnDefinition = "jsonb"
    )
    private Map<String, Object> activityData;

    // COMUNIDAD

    @Column(name = "community_id")
    private UUID communityId;

    // FECHAS

    @Column(
        name = "created_at",
        nullable = false
    )
    private OffsetDateTime createdAt;


    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;


    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // CALLBACKS JPA

    @PrePersist
    public void prePersist() {

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (postOrigin == null) {
            postOrigin = PostOrigin.USER_CREATED;
        }
    }


    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}