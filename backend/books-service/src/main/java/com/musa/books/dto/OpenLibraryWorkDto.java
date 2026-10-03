package com.musa.books.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

public class OpenLibraryWorkDto {

    private String key;

    private String title;

    private JsonNode description;

    private JsonNode authors;

    private List<Integer> covers;

    private List<String> subjects;

    @JsonProperty("first_publish_date")
    private String firstPublishDate;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getDescription() {
        return description;
    }

    public void setDescription(JsonNode description) {
        this.description = description;
    }

    public JsonNode getAuthors() {
        return authors;
    }

    public void setAuthors(JsonNode authors) {
        this.authors = authors;
    }

    public List<Integer> getCovers() {
        return covers;
    }

    public void setCovers(List<Integer> covers) {
        this.covers = covers;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public String getFirstPublishDate() {
        return firstPublishDate;
    }

    public void setFirstPublishDate(String firstPublishDate) {
        this.firstPublishDate = firstPublishDate;
    }
}