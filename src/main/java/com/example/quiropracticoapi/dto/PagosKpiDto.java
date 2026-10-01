package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagosKpiDto {
    private Double totalCobrado;
    private Long cantidadVentas;
    private Double totalEfectivo;
    private Long cantidadEfectivo;
    private Double totalBonos;
    private Long cantidadBonos;
    private Double porcentajeBonos;
    private Double totalPendiente;
    private Long cantidadPendientes;
}
