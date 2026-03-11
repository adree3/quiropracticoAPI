package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitasKpiDto {
    private long total;
    private long programadas;
    private long completadas;
    private long canceladas;
    private long ausentes;
}
