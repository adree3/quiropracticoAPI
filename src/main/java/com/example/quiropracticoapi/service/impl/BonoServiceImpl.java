package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.service.BonoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BonoServiceImpl implements BonoService {

    private final BonoActivoRepository bonoActivoRepository;
    private final AuditoriaServiceImpl auditoriaServiceImpl;

    @Autowired
    public BonoServiceImpl(BonoActivoRepository bonoActivoRepository, AuditoriaServiceImpl auditoriaServiceImpl) {
        this.bonoActivoRepository = bonoActivoRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    @Override
    public List<BonoSeleccionDto> getBonosUsables(Integer idCliente) {
        return bonoActivoRepository.findBonosDisponiblesParaCliente(idCliente).stream()
                .map(b -> {
                    BonoSeleccionDto dto = new BonoSeleccionDto();
                    dto.setIdBonoActivo(b.getIdBonoActivo());
                    dto.setNombreServicio(b.getServicioComprado().getNombreServicio());
                    dto.setSesionesRestantes(b.getSesionesRestantes());

                    // Determinar nombre a mostrar
                    if (b.getCliente().getIdCliente().equals(idCliente)) {
                        dto.setPropietarioNombre(b.getCliente().getNombre() + " (Propio)");
                        dto.setEsPropio(true);
                    } else {
                        dto.setPropietarioNombre(b.getCliente().getNombre() + " (Familiar)");
                        dto.setEsPropio(false);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void devolverSesion(Integer idBonoActivo) {
        BonoActivo bono = bonoActivoRepository.findById(idBonoActivo)
                .orElseThrow(() -> new RuntimeException("Error crítico: Se intenta devolver sesión a un bono inexistente ID: " + idBonoActivo));
        int saldoAnterior = bono.getSesionesRestantes();
        bono.setSesionesRestantes(bono.getSesionesRestantes() + 1);

        bonoActivoRepository.save(bono);
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.EDITAR,
                "BONO",
                bono.getIdBonoActivo().toString(),
                "Sesión devuelta al bono (normalmente por cancelación de cita). " +
                        "Cliente: " + bono.getCliente().getNombre() + " " + bono.getCliente().getApellidos() +
                        ". Saldo actualizado: " + saldoAnterior + " -> " + bono.getSesionesRestantes()
        );
    }
}
