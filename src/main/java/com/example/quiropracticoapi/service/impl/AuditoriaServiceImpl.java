package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.model.Auditoria;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.AuditoriaRepository;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class AuditoriaServiceImpl {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtiene los logs filtrados procesando la logica de fechas y agrupaciones
     * @param entidad (Login, Vacaciones,Horario...)
     * @param accionStr (Eliminar, Crear, Editar...)
     * @param search lo que se busca por texto plano
     * @param fechaDesde fecha de inicio por la que se filtra
     * @param fechaHasta fecha de fin hasta la que se filtra
     * @param pageable indicar que es pageable
     * @return un page de auditoria filtrados (si es necesario)
     */
    public Page<Auditoria> obtenerLogs(String entidad, String accionStr, String search, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable) {
        LocalDateTime fechaInicio = (fechaDesde != null) ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaFin = (fechaHasta != null) ? fechaHasta.atTime(23, 59, 59) : null;

        // Agrupar eliminar logico y fisico
        List<TipoAccion> accionesFiltro = null;
        if (accionStr != null && !accionStr.trim().isEmpty()) {
            try {
                if (accionStr.equalsIgnoreCase("ELIMINAR")) {
                    accionesFiltro = Arrays.asList(TipoAccion.ELIMINAR_FISICO, TipoAccion.ELIMINAR_LOGICO);
                } else {
                    accionesFiltro = Collections.singletonList(TipoAccion.valueOf(accionStr.toUpperCase()));
                }
            } catch (IllegalArgumentException e) {
                accionesFiltro = null;
            }
        }
        return auditoriaRepository.buscarConFiltros(entidad, accionesFiltro, fechaInicio, fechaFin, search, pageable);
    }

    /**
     * Registra la accion realizada en segundo plano
     * @param accion (eliminar, crear...)
     * @param entidad (cita, persona...)
     * @param idEntidad identificador de la entidad
     * @param detalles detalles extra
     */
    @Async
    public void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles) {
        registrarAccion(accion, entidad, idEntidad, detalles, null, null);
    }

    @Async
    public void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles, String usernameForzado) {
        registrarAccion(accion, entidad, idEntidad, detalles, usernameForzado, null);
    }

    /**
     * Registra la acción con resumen legible para el usuario y detalles técnicos en JSON.
     * @param resumen texto corto legible (columna Detalle en la tabla del frontend)
     */
    @Async
    public void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles, String usernameForzado, String resumen) {
        try {
            String username;

            if (usernameForzado != null) {
                username = usernameForzado;
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
                if ("anonymousUser".equals(username)) username = "SISTEMA";
            }

            Integer idUsuario = null;
            if (!username.startsWith("SISTEMA")) {
                Usuario u = usuarioRepository.findByUsername(username).orElse(null);
                if (u != null) idUsuario = u.getIdUsuario();
            }
            Auditoria log = Auditoria.builder()
                    .fechaHora(LocalDateTime.now())
                    .idUsuarioResponsable(idUsuario)
                    .usernameResponsable(username)
                    .accion(accion)
                    .entidad(entidad)
                    .idEntidad(idEntidad)
                    .resumen(resumen)
                    .detalles(detalles)
                    .build();

            auditoriaRepository.save(log);
        } catch (Exception e) {
            System.err.println("Error guardando auditoría: " + e.getMessage());
        }
    }
}