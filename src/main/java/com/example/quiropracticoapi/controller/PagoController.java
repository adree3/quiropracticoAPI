package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Gestión de Pagos", description = "Caja y ventas")
public class PagoController {

    private final PagoService pagoService;

    @Autowired
    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Registra el pago y asigna el saldo de sesiones al cliente
     * @param request datos de la venta
     * @return el estado de la venta
     */
    @Operation(summary = "Vender un bono", description = "Registra el pago y asigna el saldo de sesiones al cliente.")
    @PostMapping("/venta-bono")
    public ResponseEntity<Void> venderBono(@Valid @RequestBody VentaBonoRequestDto request) {
        pagoService.registrarVentaBono(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
