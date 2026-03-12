package com.example.quiropracticoapi.dto;

import java.time.LocalDateTime;

public interface ClienteDetalleProjection {
    Integer getIdCliente();
    String getNombre();
    String getApellidos();
    String getEmail();
    String getTelefono();
    Boolean getActivo();
    LocalDateTime getFechaAlta();
    
    // Campos calculados para evitar N+1
    Integer getCountCitasPendientes();
    Integer getCountBonosActivos();
    Boolean getTieneFamiliares();
    LocalDateTime getUltimaCita();
}
