package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClinicaSearchDto {
    private Long id;
    private String nombre;
    private String direccion;
}
