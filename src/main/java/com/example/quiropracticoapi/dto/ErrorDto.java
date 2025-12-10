package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto {
    private String message;
    private String errorType;
    private LocalDateTime timestamp;
    private int status;

    public ErrorDto(String message, String errorType, int status) {
        this.message = message;
        this.errorType = errorType;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}
