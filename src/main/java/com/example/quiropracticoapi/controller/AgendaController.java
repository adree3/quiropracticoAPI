package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.BloqueoAgendaDto;
import com.example.quiropracticoapi.service.AgendaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agenda/bloqueos")
@Tag(name = "Gestión de Agenda", description = "Vacaciones, Festivos y Bloqueos")
public class AgendaController {
    private final AgendaService agendaService;

    @Autowired
    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    /**
     * Crea un bloqueo en la agenda con los datos recibidos
     * @param dto informacion del bloqueo
     * @return respuesta de crear el bloqueo
     */
    @PostMapping
    public ResponseEntity<BloqueoAgendaDto> crearBloqueo(@RequestBody BloqueoAgendaDto dto) {
        return ResponseEntity.ok(agendaService.crearBloqueo(dto));
    }

    /**
     * Obtiene los bloqueos futuros
     * @return lista de bloqueos futuros
     */
    @GetMapping
    public ResponseEntity<List<BloqueoAgendaDto>> getFuturos() {
        return ResponseEntity.ok(agendaService.getAllBloqueos());
    }

    /**
     * Elimina el bloqueo pasado por parametro
     * @param id identificador del bloqueo
     * @return respuesta de borrar el bloqueo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Integer id) {
        agendaService.borrarBloqueo(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Actualiza un bloqueo, ya puede ser para un usuario o para todos
     * @param id identificador del bloqueo
     * @param dto datos para modificar el bloqueo
     * @return bloqueo editado
     */
    @PutMapping("/{id}")
    public ResponseEntity<BloqueoAgendaDto> actualizarBloqueo(@PathVariable Integer id, @RequestBody BloqueoAgendaDto dto) {
        return ResponseEntity.ok(agendaService.actualizarBloqueo(id, dto));
    }
}
