package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.BalanceDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
     * Obtiene los pagos pagados o pendientes
     * @param fechaInicio inicio del rango
     * @param fechaFin fin del rango
     * @param pagado indica si el pago esta pendiente o no
     * @param page numero de pagina
     * @param search buscador opcional para filtrado
     * @param size numero de registros
     * @return Page con los pagos
     */
    @Operation(summary = "Obtener lista de pagos (Paginada y Filtrada)", description = "Filtra por fechas, estado y búsqueda por texto (nombre/servicio)")
    @GetMapping
    public ResponseEntity<Page<PagoDto>> getPagos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "true") boolean pagado,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(pagoService.getPagos(fechaInicio, fechaFin, pagado, search, page, size));
    }

    /**
     * Obtiene el total cobrado y el total pendiente
     * @param fechaInicio inicio del rango
     * @param fechaFin fin del rango
     * @return el total cobrado y pendiente
     */
    @Operation(summary = "Obtener balance financiero", description = "Devuelve totales cobrados (en rango) y pendientes (globales)")
    @GetMapping("/balance")
    public ResponseEntity<BalanceDto> getBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin
    ) {
        return ResponseEntity.ok(pagoService.getBalance(fechaInicio, fechaFin));
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
