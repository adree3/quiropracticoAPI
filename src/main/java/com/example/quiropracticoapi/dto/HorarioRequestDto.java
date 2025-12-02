package com.example.quiropracticoapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HorarioRequestDto {
    @NotNull(message = "El quiropráctico es obligatorio")
    private Integer idQuiropractico;

    @NotNull(message = "El día de la semana es obligatorio")
    @Min(1) @Max(7)
    private Integer diaSemana;

    @NotNull(message = "Hora inicio obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "Hora fin obligatoria")
    private LocalTime horaFin;
}
