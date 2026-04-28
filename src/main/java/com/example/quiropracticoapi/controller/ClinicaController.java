package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ClinicaSearchDto;
import com.example.quiropracticoapi.repository.ClinicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/clinicas")
public class ClinicaController {

    private final ClinicaRepository clinicaRepository;

    @Autowired
    public ClinicaController(ClinicaRepository clinicaRepository) {
        this.clinicaRepository = clinicaRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClinicaSearchDto>> search(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<ClinicaSearchDto> result = clinicaRepository.findByNombreContainingIgnoreCaseAndActivaTrue(query.trim())
                .stream()
                .filter(c -> c.getIdClinica() != 0L)
                .map(c -> ClinicaSearchDto.builder()
                        .id(c.getIdClinica())
                        .nombre(c.getNombre())
                        .direccion(c.getDireccion())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
