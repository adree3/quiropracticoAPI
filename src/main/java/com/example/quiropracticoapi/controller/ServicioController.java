package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import com.example.quiropracticoapi.repository.ServicioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "Catálogo de Servicios", description = "Lista de precios y bonos")
public class ServicioController {
    private final ServicioRepository servicioRepository;

    @Autowired
    public ServicioController(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    /**
     * Obtiene solo los bonos activos para la venta
     * @return lista de bonos disponibles
     */
    @GetMapping("/bonos")
    public ResponseEntity<List<Servicio>> getBonosDisponibles() {
        return ResponseEntity.ok(servicioRepository.findByActivoTrueAndTipo(TipoServicio.bono));
    }
}
