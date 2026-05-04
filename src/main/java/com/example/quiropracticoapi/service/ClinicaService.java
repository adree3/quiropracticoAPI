package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.ClinicaSearchDto;
import com.example.quiropracticoapi.dto.ClinicaSettingsDto;

import java.util.List;

public interface ClinicaService {
    List<ClinicaSearchDto> searchClinicas(String query);
    ClinicaSettingsDto getClinicaSettings(Long clinicaId);
    ClinicaSettingsDto updateClinicaSettings(Long clinicaId, ClinicaSettingsDto settingsDto);
}
