package com.example.quiropracticoapi.controller;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.PagoResponseDto;
import com.example.quiropracticoapi.dto.PagosKpiDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
     * Obtiene la lista de pagos paginada con filtrado dinámico en BD por fechas, estado y búsqueda.
     */
    @Operation(summary = "Obtener lista de pagos (Paginada y Filtrada)", description = "Filtra por fechas, estado y búsqueda por texto con paginación pura en base de datos")
    @GetMapping
    public ResponseEntity<Page<PagoResponseDto>> getPagos(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(value = "pagado", required = false) Boolean pagado,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LocalDateTime desde = fechaInicio != null ? fechaInicio : inicio;
        LocalDateTime hasta = fechaFin != null ? fechaFin : fin;
        return ResponseEntity.ok(pagoService.getPagos(desde, hasta, pagado, search, pageable));
    }

    /**
     * Obtiene los KPIs financieros calculados mediante agregaciones puras en base de datos (SUM/COUNT).
     */
    @Operation(summary = "Obtener KPIs financieros", description = "Calcula métricas financieras agregadas en BD (cobrado, efectivo hoy, ventas bonos, deuda)")
    @GetMapping("/kpis")
    public ResponseEntity<PagosKpiDto> getKpis(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        LocalDateTime desde = fechaInicio != null ? fechaInicio : inicio;
        LocalDateTime hasta = fechaFin != null ? fechaFin : fin;
        return ResponseEntity.ok(pagoService.getKpis(desde, hasta));
    }

    /**
     * Endpoint legacy de balance, manteniendo retrocompatibilidad y enriquecido con métricas BD.
     */
    @Operation(summary = "Obtener balance financiero", description = "Devuelve totales cobrados y pendientes enriquecidos")
    @GetMapping("/balance")
    public ResponseEntity<PagosKpiDto> getBalance(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(value = "fin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        LocalDateTime desde = fechaInicio != null ? fechaInicio : inicio;
        LocalDateTime hasta = fechaFin != null ? fechaFin : fin;
        return ResponseEntity.ok(pagoService.getKpis(desde, hasta));
    }

    @Operation(summary = "Obtener pagos de un cliente específico")
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<PagoDto>> getPagosCliente(@PathVariable Integer idCliente) {
        return ResponseEntity.ok(pagoService.getPagosCliente(idCliente));
    }

    @Operation(summary = "Vender un bono", description = "Registra el pago y asigna el saldo de sesiones al cliente.")
    @PostMapping("/venta-bono")
    public ResponseEntity<Void> venderBono(@Valid @RequestBody VentaBonoRequestDto request) {
        pagoService.registrarVentaBono(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarPago(@PathVariable Integer id) {
        pagoService.confirmarPago(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/pendiente")
    public ResponseEntity<Void> pendientePago(@PathVariable Integer id) {
        pagoService.pendientePago(id);
        return ResponseEntity.ok().build();
    }
}
