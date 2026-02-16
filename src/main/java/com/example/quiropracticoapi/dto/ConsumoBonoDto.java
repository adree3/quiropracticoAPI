package com.example.quiropracticoapi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumoBonoDto {
    private Integer idConsumo;
    private LocalDateTime fechaConsumo;
    private Integer sesionesRestantesSnapshot;
    
    // Datos de la Cita asociada
    private Integer idCita;
    private Integer idPaciente;
    private LocalDateTime fechaCita;
    private String nombreQuiropractico;
    private String nombrePaciente;
    private String estadoCita;
}
