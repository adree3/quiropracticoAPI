package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HistorialDto {
    private Integer idHistorial;
    private Integer idCita;
    private String notasSubjetivo;
    private String notasObjetivo;
    private String ajustesRealizados;
    private String planFuturo;
}
