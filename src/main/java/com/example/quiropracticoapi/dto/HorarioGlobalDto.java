package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HorarioGlobalDto {
    private Integer idHorario;
    private Integer idQuiropractico;
    private Byte diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
