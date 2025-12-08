package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BonoSeleccionDto {
    private Integer idBonoActivo;
    private String nombreServicio;
    private int sesionesRestantes;
    private String propietarioNombre;
    private boolean esPropio;
}
