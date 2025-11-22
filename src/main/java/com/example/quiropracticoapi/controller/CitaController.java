package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.CitaDto;
import com.example.quiropracticoapi.dto.CitaRequestDto;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "Gestión de Citas", description = "Endpoints para reservar, cancelar y ver citas")
public class CitaController {
    private final CitaService citaService;

    @Autowired
    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    // Crear cita
    @Operation(summary = "Crear una nueva cita", description = "Valida horarios, bloqueos y solapamientos antes de guardar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Horario no disponible, bloqueo activo o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente o Quiropráctico no encontrados")
    })
    @PostMapping
    public ResponseEntity<CitaDto> crearCita(@Valid @RequestBody CitaRequestDto request) {
        CitaDto nuevaCita = citaService.crearCita(request);
        return new ResponseEntity<>(nuevaCita, HttpStatus.CREATED);
    }
    // Ver Agenda del día
    @Operation(summary = "Ver agenda de un día específico", description = "Devuelve todas las citas de la clínica para una fecha dada.")
    @GetMapping("/agenda")
    public ResponseEntity<List<CitaDto>> getAgendaDiaria(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        // Ejemplo GET /api/citas/agenda?fecha=2025-11-25
        List<CitaDto> citas = citaService.getCitasPorFecha(fecha);
        return ResponseEntity.ok(citas);
    }

    // Historial de un Cliente
    @Operation(summary = "Ver citas de un cliente", description = "Devuelve el historial completo de citas de un paciente.")
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<CitaDto>> getCitasCliente(@PathVariable Integer idCliente) {
        List<CitaDto> citas = citaService.getCitasPorCliente(idCliente);
        return ResponseEntity.ok(citas);
    }

    @Operation(summary = "Obtener detalles de una cita", description = "Devuelve la información de una cita específica.")
    @GetMapping("/{idCita}")
    public ResponseEntity<CitaDto> getCitaById(@PathVariable Integer idCita) {
        CitaDto cita = citaService.getCitaById(idCita);
        return ResponseEntity.ok(cita);
    }

    // Cancelar Cita
    @Operation(summary = "Cancelar una cita", description = "Cambia el estado de la cita a 'cancelada'. No la borra de la base de datos.")
    @PutMapping("/{idCita}/cancelar")
    public ResponseEntity<Void> cancelarCita(@PathVariable Integer idCita) {
        citaService.cancelarCita(idCita);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cambiar estado de la cita", description = "Permite marcar una cita como COMPLETADA o AUSENTE.")
    @PatchMapping("/{idCita}/estado")
    public ResponseEntity<CitaDto> cambiarEstadoCita(
            @PathVariable Integer idCita,
            @RequestParam EstadoCita nuevoEstado) {

        CitaDto citaActualizada = citaService.cambiarEstado(idCita, nuevoEstado);
        return ResponseEntity.ok(citaActualizada);
    }
}
