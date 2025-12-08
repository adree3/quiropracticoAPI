package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Obtiene los pagos por rango de fechas
     * @param inicio fecha inicio
     * @param fin fecha fin
     * @return lista de pagos
     */
    @GetMapping
    public ResponseEntity<List<PagoDto>> getPagosRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        return ResponseEntity.ok(pagoService.getPagosEnRango(inicio, fin));
    }

    /**
     * Obtiene los pagos pendientes
     * @return lista de pagos pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<PagoDto>> getPendientes() {
        return ResponseEntity.ok(pagoService.getPagosPendientes());
    }

    /**
     * Confirma que un pago se ha realizado
     * @param id identificador del pago
     * @return respuesta de la operacion
     */
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarPago(@PathVariable Integer id) {
        pagoService.confirmarPago(id);
        return ResponseEntity.ok().build();
    }
}
