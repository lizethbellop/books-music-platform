package com.musa.books.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenLibrarySearchResponse {

    @JsonProperty("numFound")
    private Integer numFound;

    private List<OpenLibraryBookDto> docs;

    public Integer getNumFound() {
        return numFound;
    }

    public void setNumFound(Integer numFound) {
        this.numFound = numFound;
    }

    public List<OpenLibraryBookDto> getDocs() {
        return docs;
    }

    public void setDocs(List<OpenLibraryBookDto> docs) {
        this.docs = docs;
    }
}