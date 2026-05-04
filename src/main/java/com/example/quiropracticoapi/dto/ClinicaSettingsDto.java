package com.example.quiropracticoapi.dto;

import lombok.Data;

@Data
public class ClinicaSettingsDto {
    private String nombre;
    private String cifNif;
    private String telefono;
    private String emailContacto;
    private String direccion;
    private Integer duracionCitaMinutos;
    private Long limiteAlmacenamientoBytes;
    private Long almacenamientoUsadoBytes;
}
