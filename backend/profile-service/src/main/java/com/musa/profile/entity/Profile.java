package com.musa.profile.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 500)
    private String biography;

    @Column(name = "profile_picture_url", length = 1000)
    private String profilePictureUrl;

    @Column(name = "profile_picture_public_id", length = 255)
    private String profilePicturePublicId;

    @Column(name = "is_private", nullable = false)
    private boolean privateProfile;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Profile(){

    }

    public Profile(UUID userId, boolean privateProfile){
        this.userId = userId;
        this.privateProfile = privateProfile;
    }

    @PrePersist
    void beforeInsert(){
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate(){
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getProfilePicturePublicId() {
        return profilePicturePublicId;
    }

    public void setProfilePicturePublicId(String profilePicturePublicId) {
        this.profilePicturePublicId = profilePicturePublicId;
    }

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
