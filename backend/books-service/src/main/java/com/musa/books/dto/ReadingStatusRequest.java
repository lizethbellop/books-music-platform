package com.musa.books.dto;

import com.musa.books.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;

public class ReadingStatusRequest {

    @NotNull(message = "El estado de lectura es obligatorio")
    private ReadingStatus status;

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }
}