package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.HorarioDto;
import com.example.quiropracticoapi.dto.HorarioRequestDto;
import com.example.quiropracticoapi.service.HorarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@Tag(name = "Gestión de Horarios", description = "Definir turnos de trabajo")
public class HorarioController {
    private final HorarioService horarioService;

    @Autowired
    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    /**
     * Devuelve una lista con los horarios de un doctor
     * @param idQuiro identificador
     * @return lista de horarios
     */
    @GetMapping("/quiro/{idQuiro}")
    public ResponseEntity<List<HorarioDto>> getByQuiro(@PathVariable Integer idQuiro) {
        return ResponseEntity.ok(horarioService.getHorariosByQuiropractico(idQuiro));
    }

    /**
     * Crea un horario
     * @param request datos del turno
     * @return respuesta de crear el turno
     */
    @PostMapping
    public ResponseEntity<HorarioDto> create(@Valid @RequestBody HorarioRequestDto request) {
        return ResponseEntity.ok(horarioService.createHorario(request));
    }

    /**
     * Elimina turno por id
     * @param id identificador del horario
     * @return respuesta de eliminar el turno
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        horarioService.deleteHorario(id);
        return ResponseEntity.ok().build();
    }
}
