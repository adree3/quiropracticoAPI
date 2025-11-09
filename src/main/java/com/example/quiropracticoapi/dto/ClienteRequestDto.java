package com.example.quiropracticoapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClienteRequestDto {
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos no puede estar vacíos")
    @Size(max = 150, message = "Los apellidos no pueden tener más de 150 caracteres")
    private String apellidos;

    @NotBlank(message = "El teléfono no puede estar vacío")
    @Size(max = 25)
    private String telefono;

    @Email(message = "El formato del email no es válido")
    @Size(max = 100)
    private String email;

    private LocalDate fechaNacimiento;

    private String direccion;

    private String notasPrivadas;
}
