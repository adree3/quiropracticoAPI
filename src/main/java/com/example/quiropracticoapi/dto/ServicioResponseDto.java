package com.example.quiropracticoapi.dto;

public record ServicioResponseDto(
    Long idServicio,
    String nombreServicio,
    Double precio,
    String tipo,
    Integer sesionesIncluidas,
    Boolean activo
) {}
