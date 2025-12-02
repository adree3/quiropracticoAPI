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
public class HorarioDto {
    private Integer idHorario;
    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
