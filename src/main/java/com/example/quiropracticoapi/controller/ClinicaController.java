package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ClinicaSearchDto;
import com.example.quiropracticoapi.service.ClinicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/clinicas")
public class ClinicaController {

    private final ClinicaService clinicaService;

    @Autowired
    public ClinicaController(ClinicaService clinicaService) {
        this.clinicaService = clinicaService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClinicaSearchDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(clinicaService.searchClinicas(query));
    }
}
