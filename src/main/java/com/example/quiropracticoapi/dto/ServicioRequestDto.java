package com.example.quiropracticoapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServicioRequestDto {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreServicio;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotBlank(message = "El tipo es obligatorio (sesion_unica o bono)")
    private String tipo; // 'sesion_unica' o 'bono'

    private Integer sesionesIncluidas;
}
