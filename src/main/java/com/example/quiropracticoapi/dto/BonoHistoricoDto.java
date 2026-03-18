package com.example.quiropracticoapi.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BonoHistoricoDto {
    private Integer idBonoActivo;
    private Integer idCliente;
    private String nombreCliente;
    private String nombreServicio;
    private int sesionesTotales;
    private int sesionesRestantes;
    private LocalDate fechaCompra;
    private LocalDate fechaCaducidad;
    private boolean pagado;
}
