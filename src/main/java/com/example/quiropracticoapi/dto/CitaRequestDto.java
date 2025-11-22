package com.example.quiropracticoapi.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CitaRequestDto {
    @NotNull(message = "El ID del cliente es obligatorio")
    private Integer idCliente;

    @NotNull(message = "El ID del quiropráctico es obligatorio")
    private Integer idQuiropractico;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La cita debe ser en el futuro")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaHoraFin;

    private String notasRecepcion;

    private Integer idBonoAUtilizar;
    // Si viene NULL -> Lógica automática (usar el mío más antiguo).
    // Si viene con ID -> Forzar el uso de ESE bono (verificando que tengo permiso).
}
