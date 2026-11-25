package com.musa.social.entity;

import jakarta.persistence.*;

import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "moderation_term_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationTermVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "variant", nullable = false, length = 150)
    private String variant;

    @Column(
            name = "normalized_variant",
            nullable = false,
            unique = true,
            length = 150
    )
    private String normalizedVariant;
}