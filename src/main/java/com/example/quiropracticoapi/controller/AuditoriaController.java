package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.model.Auditoria;
import com.example.quiropracticoapi.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    @Autowired
    public AuditoriaController(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Obtiene todos los logs paginados (filtrados si se indica)
     * @param entidad (Crear, eliminar)
     * @param search el texto que quiere encontrar
     * @param fecha fecha para los logs
     * @param page numero de pagina para obtener
     * @param size el tamaño de cada página
     * @param sortBy ordenado por fecha
     * @param direction primero el mas nuevo
     * @return la pagina con los logs filtrados si se ha indicado
     */
    @GetMapping
    public ResponseEntity<Page<Auditoria>> getLogs(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaHora") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Procesar fechas (Inicio y Fin del día seleccionado)
        LocalDateTime fechaInicio = null;
        LocalDateTime fechaFin = null;

        if (fecha != null && !fecha.isEmpty()) {
            LocalDate date = LocalDate.parse(fecha); // Espera "2023-12-18"
            fechaInicio = date.atStartOfDay();
            fechaFin = date.atTime(23, 59, 59);
        }
        return ResponseEntity.ok(
                auditoriaRepository.buscarConFiltros(entidad, fechaInicio, fechaFin, search, pageable)
        );
    }
}