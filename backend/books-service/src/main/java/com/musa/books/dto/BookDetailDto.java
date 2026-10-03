package com.musa.books.dto;

import java.util.List;

public class BookDetailDto {

    private String externalId;
    private String title;
    private List<String> authors;
    private String description;
    private String firstPublishDate;
    private List<String> subjects;
    private String coverUrl;

    public BookDetailDto() {
    }

    public BookDetailDto(
            String externalId,
            String title,
            List<String> authors,
            String description,
            String firstPublishDate,
            List<String> subjects,
            String coverUrl
    ) {
        this.externalId = externalId;
        this.title = title;
        this.authors = authors;
        this.description = description;
        this.firstPublishDate = firstPublishDate;
        this.subjects = subjects;
        this.coverUrl = coverUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFirstPublishDate() {
        return firstPublishDate;
    }

    public void setFirstPublishDate(String firstPublishDate) {
        this.firstPublishDate = firstPublishDate;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}