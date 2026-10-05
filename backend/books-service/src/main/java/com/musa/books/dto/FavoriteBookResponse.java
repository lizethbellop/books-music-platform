package com.musa.books.dto;

public class FavoriteBookResponse {

    private String externalId;
    private String title;
    private String author;
    private String coverUrl;

    public FavoriteBookResponse(
            String externalId,
            String title,
            String author,
            String coverUrl
    ) {
        this.externalId = externalId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }
}