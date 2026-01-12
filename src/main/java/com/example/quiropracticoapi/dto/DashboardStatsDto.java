package com.example.quiropracticoapi.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {
    private Double ingresosHoy;
    private Double ingresosMes;

    private long citasHoyTotal;
    private long citasHoyPendientes;

    private long nuevosClientesMes;
    private long totalClientesActivos;

    private List<ChartDataDto> graficaIngresos;
}
