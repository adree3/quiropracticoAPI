package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BonoDto {
    private Integer idBonoActivo;
    private String nombreServicio;
    private int sesionesTotales;
    private int sesionesRestantes;
    private LocalDate fechaCaducidad;
}
