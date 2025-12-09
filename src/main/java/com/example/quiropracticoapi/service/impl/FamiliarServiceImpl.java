package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.CitaConflictoDto;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.GrupoFamiliar;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.GrupoFamiliarRepository;
import com.example.quiropracticoapi.service.BonoService;
import com.example.quiropracticoapi.service.FamiliarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FamiliarServiceImpl implements FamiliarService {
    private final CitaRepository citaRepository;
    private final GrupoFamiliarRepository grupoFamiliarRepository;
    private final BonoService bonoService;

    @Autowired
    public FamiliarServiceImpl(CitaRepository citaRepository, GrupoFamiliarRepository grupoFamiliarRepository, BonoService bonoService) {
        this.citaRepository = citaRepository;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.bonoService = bonoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaConflictoDto> obtenerCitasConflictivas(Integer idFamiliar) {
        GrupoFamiliar grupo = grupoFamiliarRepository.findById(idFamiliar)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        Integer idBeneficiario = grupo.getBeneficiario().getIdCliente();
        Integer idPropietario = grupo.getPropietario().getIdCliente();

        List<Cita> citas = citaRepository.findCitasFuturasConBonoPrestado(idBeneficiario, idPropietario);

        return citas.stream()
                .map(c -> new CitaConflictoDto(
                        c.getIdCita(),
                        c.getFechaHoraInicio(),
                        c.getConsumoBono().getBonoActivo().getServicioComprado().getNombreServicio()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void desvincularFamiliar(Integer idGrupo, List<Integer> idsCitasACancelar) {
        if (idsCitasACancelar != null && !idsCitasACancelar.isEmpty()) {
            List<Cita> citas = citaRepository.findAllById(idsCitasACancelar);

            for (Cita cita : citas) {
                if (cita.getConsumoBono() != null) {
                    Integer idBono = cita.getConsumoBono().getBonoActivo().getIdBonoActivo();
                    bonoService.devolverSesion(idBono);
                    cita.setEstado(EstadoCita.cancelada);
                    String notaAuto = "Cita cancelada por familiar al cancelar el método de pago ";

                    if (cita.getNotasRecepcion() == null || cita.getNotasRecepcion().isEmpty()) {
                        cita.setNotasRecepcion(notaAuto);
                    } else {
                        cita.setNotasRecepcion(cita.getNotasRecepcion() + "\n" + notaAuto);
                    }
                }
            }
            citaRepository.saveAll(citas);
        }

        // 2. Borrar relación familiar
        grupoFamiliarRepository.deleteById(idGrupo);
    }
}
