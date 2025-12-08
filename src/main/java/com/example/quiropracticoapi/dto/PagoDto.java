package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PagoDto {
    private Integer idPago;
    private String nombreCliente;
    private String concepto;
    private BigDecimal monto;
    private String metodoPago;
    private LocalDateTime fechaPago;
    private boolean pagado;
}
