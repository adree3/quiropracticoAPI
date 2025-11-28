package com.example.quiropracticoapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VentaBonoRequestDto {
    private Integer idCliente;

    @NotNull(message = "El servicio (bono) es obligatorio")
    private Integer idServicio;

    @NotNull(message = "El método de pago es obligatorio")
    private String metodoPago;
}
