package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ClinicaSettingsDto;
import com.example.quiropracticoapi.service.ClinicaService;
import com.example.quiropracticoapi.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clinicas/settings")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ClinicaSettingsController {

    private final ClinicaService clinicaService;

    @Autowired
    public ClinicaSettingsController(ClinicaService clinicaService) {
        this.clinicaService = clinicaService;
    }

    @GetMapping
    public ResponseEntity<ClinicaSettingsDto> getSettings() {
        Long clinicaId = TenantContext.getTenantId();
        return ResponseEntity.ok(clinicaService.getClinicaSettings(clinicaId));
    }

    @PutMapping
    public ResponseEntity<ClinicaSettingsDto> updateSettings(@RequestBody ClinicaSettingsDto dto) {
        Long clinicaId = TenantContext.getTenantId();
        return ResponseEntity.ok(clinicaService.updateClinicaSettings(clinicaId, dto));
    }
}
