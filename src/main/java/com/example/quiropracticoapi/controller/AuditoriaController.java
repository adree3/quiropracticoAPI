package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.model.Auditoria;
import com.example.quiropracticoapi.repository.AuditoriaRepository;
import com.example.quiropracticoapi.service.AuditoriaService;
import com.example.quiropracticoapi.service.impl.AuditoriaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaServiceImpl auditoriaServiceImpl;

    @Autowired
    public AuditoriaController(AuditoriaServiceImpl auditoriaServiceImpl) {
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    /**
     * Obtiene todos los logs paginados (filtrados si se indica)
     * @param entidad (Crear, eliminar)
     * @param search el texto que quiere encontrar
     * @param fechaDesde fecha inicio para filtrar logs
     * @param fechaHasta fecha fin para filtrar logs
     * @param page numero de pagina para obtener
     * @param size el tamaño de cada página
     * @param sortBy ordenado por fecha
     * @param direction primero el mas nuevo
     * @return la pagina con los logs filtrados si se ha indicado
     */
    @GetMapping
    public ResponseEntity<Page<Auditoria>> getLogs(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaHora") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                auditoriaServiceImpl.obtenerLogs(entidad, accion, search, fechaDesde, fechaHasta, pageable)
        );
    }
}