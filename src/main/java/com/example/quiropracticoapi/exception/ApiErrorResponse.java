package com.example.quiropracticoapi.exception;

public record ApiErrorResponse(int status, String codigo, String mensaje, String detalles) {}
