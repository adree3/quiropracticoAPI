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
public class BloqueoAgendaDto {
    private Integer idBloqueo;
    private Integer idQuiropractico;
    private String nombreQuiropractico;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String motivo;
}
