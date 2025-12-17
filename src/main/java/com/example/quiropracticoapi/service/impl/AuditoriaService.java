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
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
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
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "SISTEMA";

            Integer idUsuario = null;

            if (!username.equals("SISTEMA") && !username.equals("anonymousUser")) {
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