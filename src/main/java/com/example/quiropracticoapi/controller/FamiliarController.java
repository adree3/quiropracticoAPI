package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.CitaConflictoDto;
import com.example.quiropracticoapi.dto.DesvinculacionRequestDto;
import com.example.quiropracticoapi.service.FamiliarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/familiares")
public class FamiliarController {


    private final FamiliarService familiarService;

    @Autowired
    public FamiliarController(FamiliarService familiarService) {
        this.familiarService = familiarService;
    }

    @GetMapping("/{idGrupo}/conflictos")
    public ResponseEntity<List<CitaConflictoDto>> obtenerConflictos(@PathVariable Integer idGrupo) {
        return ResponseEntity.ok(familiarService.obtenerCitasConflictivas(idGrupo));
    }

    @PostMapping("/{idGrupo}/desvincular")
    public ResponseEntity<Void> desvincular(@PathVariable Integer idGrupo,
                                            @RequestBody DesvinculacionRequestDto request) {
        familiarService.desvincularFamiliar(idGrupo, request.getIdsCitasACancelar());
        return ResponseEntity.noContent().build();
    }
}