package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.service.FacturacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
@Tag(name = "Facturas", description = "Gestión y descarga segura de facturas")
public class FacturaController {

    private final FacturacionService facturacionService;

    @Autowired
    public FacturaController(FacturacionService facturacionService) {
        this.facturacionService = facturacionService;
    }

    @Operation(summary = "Obtener URL prefirmada de factura por pago", description = "Genera una URL temporal prefirmada en Cloudflare R2 con trazabilidad de descarga (RGPD).")
    @GetMapping("/pagos/{idPago}/url")
    public ResponseEntity<String> obtenerUrlFacturaPorPago(@PathVariable Integer idPago) {
        return ResponseEntity.ok(facturacionService.obtenerUrlFacturaPorPago(idPago));
    }
}
