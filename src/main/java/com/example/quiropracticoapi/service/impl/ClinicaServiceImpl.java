package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.ClinicaSearchDto;
import com.example.quiropracticoapi.dto.ClinicaSettingsDto;
import com.example.quiropracticoapi.model.Clinica;
import com.example.quiropracticoapi.repository.ClinicaRepository;
import com.example.quiropracticoapi.service.ClinicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClinicaServiceImpl implements ClinicaService {

    private final ClinicaRepository clinicaRepository;
    private final AuditoriaServiceImpl auditoriaService;

    @Autowired
    public ClinicaServiceImpl(ClinicaRepository clinicaRepository, AuditoriaServiceImpl auditoriaService) {
        this.clinicaRepository = clinicaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public List<ClinicaSearchDto> searchClinicas(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return clinicaRepository.findByNombreContainingIgnoreCaseAndActivaTrue(query.trim())
                .stream()
                .filter(c -> c.getIdClinica() != 0L)
                .map(this::mapToSearchDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClinicaSettingsDto getClinicaSettings(Long clinicaId) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RuntimeException("Clínica no encontrada"));

        return mapToSettingsDto(clinica);
    }

    @Override
    @Transactional
    public ClinicaSettingsDto updateClinicaSettings(Long clinicaId, ClinicaSettingsDto settingsDto) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RuntimeException("Clínica no encontrada"));

        String oldNombre = clinica.getNombre();
        
        if (settingsDto.getNombre() != null) clinica.setNombre(settingsDto.getNombre());
        clinica.setCifNif(settingsDto.getCifNif());
        clinica.setTelefono(settingsDto.getTelefono());
        clinica.setEmailContacto(settingsDto.getEmailContacto());
        clinica.setDireccion(settingsDto.getDireccion());
        
        if (settingsDto.getDuracionCitaMinutos() != null) {
            clinica.setDuracionCitaMinutos(settingsDto.getDuracionCitaMinutos());
        }

        Clinica updated = clinicaRepository.save(clinica);

        auditoriaService.registrarAccion(
                com.example.quiropracticoapi.model.enums.TipoAccion.EDITAR,
                "CLINICA",
                clinicaId.toString(),
                "Ajustes de clínica actualizados",
                null,
                "Se han modificado los datos de configuración de la clínica: " + updated.getNombre()
        );

        return mapToSettingsDto(updated);
    }

    private ClinicaSearchDto mapToSearchDto(Clinica clinica) {
        return ClinicaSearchDto.builder()
                .id(clinica.getIdClinica())
                .nombre(clinica.getNombre())
                .direccion(clinica.getDireccion())
                .build();
    }

    private ClinicaSettingsDto mapToSettingsDto(Clinica clinica) {
        ClinicaSettingsDto dto = new ClinicaSettingsDto();
        dto.setNombre(clinica.getNombre());
        dto.setCifNif(clinica.getCifNif());
        dto.setTelefono(clinica.getTelefono());
        dto.setEmailContacto(clinica.getEmailContacto());
        dto.setDireccion(clinica.getDireccion());
        dto.setDuracionCitaMinutos(clinica.getDuracionCitaMinutos());
        dto.setLimiteAlmacenamientoBytes(clinica.getLimiteAlmacenamientoBytes());
        dto.setAlmacenamientoUsadoBytes(clinica.getAlmacenamientoUsadoBytes());
        return dto;
    }
}
