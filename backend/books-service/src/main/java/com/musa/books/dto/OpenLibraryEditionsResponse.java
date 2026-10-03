package com.musa.books.dto;

import java.util.List;

public class OpenLibraryEditionsResponse {

    private List<OpenLibraryEditionDto> entries;

    public List<OpenLibraryEditionDto> getEntries() {
        return entries;
    }

    public void setEntries(List<OpenLibraryEditionDto> entries) {
        this.entries = entries;
    }
}