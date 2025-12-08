package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.HistorialDto;
import com.example.quiropracticoapi.service.HistorialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historial")
@Tag(name = "Historial Clínico", description = "Notas SOAP")
public class HistorialController {
    private final HistorialService historialService;

    @Autowired
    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    /**
     * Obtiene el historial clinico por el id de la cita
     * @param idCita identificador de la cita
     * @return un historial
     */
    @GetMapping("/cita/{idCita}")
    public ResponseEntity<HistorialDto> getPorCita(@PathVariable Integer idCita) {
        return ResponseEntity.ok(historialService.getHistorialPorCita(idCita));
    }

    /**
     * Guarda un historial con los datos proporcionados
     * @param dto datos del historial
     * @return respuesta de crearlo
     */
    @PostMapping
    public ResponseEntity<HistorialDto> guardar(@RequestBody HistorialDto dto) {
        return ResponseEntity.ok(historialService.saveHistorial(dto));
    }
}
