package com.example.quiropracticoapi.exception;

import lombok.Getter;

@Getter
public class AgendaConflictException extends RuntimeException {
    private final String codigo;

    public AgendaConflictException(String message, String codigo) {
        super(message);
        this.codigo = codigo;
    }
}