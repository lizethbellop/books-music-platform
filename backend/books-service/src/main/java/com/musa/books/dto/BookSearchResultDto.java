package com.musa.books.dto;

import java.util.List;

public class BookSearchResultDto {

    private String externalId;
    private String title;
    private List<String> authors;
    private Integer firstPublishYear;
    private String coverUrl;

    public BookSearchResultDto() {
    }

    public BookSearchResultDto(
            String externalId,
            String title,
            List<String> authors,
            Integer firstPublishYear,
            String coverUrl
    ) {
        this.externalId = externalId;
        this.title = title;
        this.authors = authors;
        this.firstPublishYear = firstPublishYear;
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

    public Integer getFirstPublishYear() {
        return firstPublishYear;
    }

    public void setFirstPublishYear(Integer firstPublishYear) {
        this.firstPublishYear = firstPublishYear;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}