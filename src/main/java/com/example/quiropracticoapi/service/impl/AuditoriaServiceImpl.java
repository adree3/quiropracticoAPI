package com.example.quiropracticoapi.service.impl;


import com.example.quiropracticoapi.model.Auditoria;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.AuditoriaRepository;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
     * Registra la accion realizada en segundo plano
     * @param accion (eliminar, crear...)
     * @param entidad (cita, persona...)
     * @param idEntidad identificador de la entidad
     * @param detalles detalles extra
     */
    @Async
    public void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles) {
        registrarAccion(accion, entidad, idEntidad, detalles, null);
    }

    /**
     * Lo mismo que el anterior solo que implementa un campo para los login de usuarios
     * @param accion (eliminar, crear...)
     * @param entidad (cita, persona...)
     * @param idEntidad identificador de la entidad
     * @param detalles detalles extra
     * @param usernameForzado el usuario a loggearse
     */
    @Async
    public void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles, String usernameForzado) {
        try {
            String username;

            if (usernameForzado != null) {
                username = usernameForzado;
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
                if ("anonymousUser".equals(username)) username = "SISTEMA/WEB";
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
                    .detalles(detalles)
                    .build();

            auditoriaRepository.save(log);
        } catch (Exception e) {
            System.err.println("Error guardando auditoría: " + e.getMessage());
        }
    }
}