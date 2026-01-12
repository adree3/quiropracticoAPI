package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ServicioRequestDto;
import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.TipoServicio;
import com.example.quiropracticoapi.repository.ServicioRepository;
import com.example.quiropracticoapi.service.ServicioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import java.util.concurrent.TimeUnit;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "Catálogo de Servicios", description = "Lista de precios y bonos")
public class ServicioController {
    private final ServicioRepository servicioRepository;
    private final ServicioService servicioService;


    @Autowired
    public ServicioController(ServicioRepository servicioRepository, ServicioService servicioService) {
        this.servicioRepository = servicioRepository;
        this.servicioService = servicioService;
    }

    /**
     * Obtiene todos los servicios
     * @return una lista de servicios
     */
    @GetMapping
    public ResponseEntity<List<Servicio>> getAll(
            @RequestParam(required = false) Boolean activo
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(servicioService.getAllServicios(activo));
    }

    /**
     * Crea un nuevo servicio
     * @param request datos del servicio a crear
     * @return resultado de crear el servicio
     */
    @PostMapping
    public ResponseEntity<Servicio> create(@Valid @RequestBody ServicioRequestDto request) {
        return ResponseEntity.ok(servicioService.createServicio(request));
    }

    /**
     * Edita un servicio
     * @param id identificador del servicio
     * @param request datos del servicio a editar
     * @return resultado de editar el servicio
     */
    @PutMapping("/{id}")
    public ResponseEntity<Servicio> update(@PathVariable Integer id, @Valid @RequestBody ServicioRequestDto request) {
        return ResponseEntity.ok(servicioService.updateServicio(id, request));
    }

    /**
     * Desactiva el servicio "Elimina"
     * @param id identificador del servicio
     * @return resultado de "eliminarlo"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        servicioService.deleteServicio(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Activa un servicio "eliminado"
     * @param id identificador del servicio
     * @return resultado del servicio activado
     */
    @PutMapping("/{id}/recuperar")
    public ResponseEntity<Void> recover(@PathVariable Integer id) {
        servicioService.recoverServicio(id);
        return ResponseEntity.ok().build();
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
