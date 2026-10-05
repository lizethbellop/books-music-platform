package com.musa.books.dto;

import com.musa.books.enums.ReadingStatus;

public class UserBookResponse {

    private String externalId;
    private String title;
    private String author;
    private String coverUrl;
    private ReadingStatus readingStatus;

    public UserBookResponse(
            String externalId,
            String title,
            String author,
            String coverUrl,
            ReadingStatus readingStatus
    ) {
        this.externalId = externalId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.readingStatus = readingStatus;
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

    public ReadingStatus getReadingStatus() {
        return readingStatus;
    }
}