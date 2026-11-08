package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArqueoCajaDTO {

    @Builder.Default
    private BigDecimal efectivo = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tarjeta = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal transferencia = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    private Integer cantidadOperaciones = 0;
}
