package com.musa.profile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.musa.profile.entity.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfilePhotoService {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ProfileService profileService;
    private final String cloudinaryUrl;

    public ProfilePhotoService(ProfileService profileService,
                               @Value("${CLOUDINARY_URL:}") String cloudinaryUrl) {
        this.profileService = profileService;
        this.cloudinaryUrl = cloudinaryUrl;
    }

    public Profile upload(UUID userId, MultipartFile file) {
        // Check ownership before uploading anything to Cloudinary.
        profileService.getByUserId(userId);
        if (file.isEmpty() || file.getSize() > MAX_BYTES ||
                !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a JPEG, PNG or WebP image up to 5 MB.");
        }
        if (cloudinaryUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary is not configured.");
        }

        Map<?, ?> result;
        try {
            Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
            result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "musa/profiles", "resource_type", "image"));
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not upload profile photo.", ex);
        }
        return profileService.updatePhoto(userId,
                (String) result.get("secure_url"),
                (String) result.get("public_id"));
    }
}
