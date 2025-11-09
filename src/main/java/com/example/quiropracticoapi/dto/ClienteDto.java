package com.example.quiropracticoapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ClienteDto {
    private Integer idCliente;
    private LocalDateTime fechaAlta;
    private String nombre;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;
    private String notasPrivadas;
}
