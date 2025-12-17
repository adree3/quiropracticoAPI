package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.CitaConflictoDto;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.GrupoFamiliar;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.model.enums.TipoAccion;
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
    private final AuditoriaService auditoriaService;

    @Autowired
    public FamiliarServiceImpl(CitaRepository citaRepository, GrupoFamiliarRepository grupoFamiliarRepository, BonoService bonoService, AuditoriaService auditoriaService) {
        this.citaRepository = citaRepository;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.bonoService = bonoService;
        this.auditoriaService = auditoriaService;
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
        GrupoFamiliar grupo = grupoFamiliarRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Relación familiar no encontrada"));
        String nombrePropietario = grupo.getPropietario().getNombre();
        String nombreBeneficiario = grupo.getBeneficiario().getNombre();

        if (idsCitasACancelar != null && !idsCitasACancelar.isEmpty()) {
            List<Cita> citas = citaRepository.findAllById(idsCitasACancelar);
            int citasCanceladasCount = 0;

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
                    citasCanceladasCount++;
                }
            }
            citaRepository.saveAll(citas);
            if (citasCanceladasCount > 0) {
                auditoriaService.registrarAccion(
                        TipoAccion.EDITAR,
                        "CITA",
                        "VARIAS (" + citasCanceladasCount + ")",
                        "Cancelación automática de " + citasCanceladasCount + " citas futuras de " + nombreBeneficiario +
                                " por pérdida de acceso al bono de " + nombrePropietario
                );
            }
        }

        // 2. Borrar relación familiar
        grupoFamiliarRepository.deleteById(idGrupo);
        auditoriaService.registrarAccion(
                TipoAccion.ELIMINAR_FISICO,
                "GRUPO_FAMILIAR",
                idGrupo.toString(),
                "Desvinculación familiar. Propietario: " + nombrePropietario + " ya no comparte bonos con Beneficiario: " + nombreBeneficiario
        );
    }
}
