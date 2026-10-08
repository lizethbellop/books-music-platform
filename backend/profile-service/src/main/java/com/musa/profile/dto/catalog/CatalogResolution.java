package com.musa.profile.dto.catalog;

public record CatalogResolution(
        Status resolutionStatus,
        Object content
) {

    public enum Status {
        AVAILABLE,
        NOT_FOUND,
        UNAVAILABLE
    }

    public static CatalogResolution available(
            BookCatalogResponse content
    ) {
        return new CatalogResolution(Status.AVAILABLE, content);
    }

    public static CatalogResolution available(
            MusicCatalogResponse content
    ) {
        return new CatalogResolution(Status.AVAILABLE, content);
    }

    public static CatalogResolution notFound() {
        return new CatalogResolution(Status.NOT_FOUND, null);
    }

    public static CatalogResolution unavailable() {
        return new CatalogResolution(Status.UNAVAILABLE, null);
    }
}