package com.example.quiropracticoapi.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.example.quiropracticoapi.model.Usuario) {
                com.example.quiropracticoapi.model.Usuario usuario = (com.example.quiropracticoapi.model.Usuario) principal;
                return Optional.of(usuario.getUsername() + " (ID:" + usuario.getIdUsuario() + ")");
            } else if (principal instanceof String) {
                return Optional.of((String) principal);
            }
        }

        try {
            jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
            if (request != null) {
                return Optional.of("ANON_IP:" + request.getRemoteAddr());
            }
        } catch (Exception e) {
            // No estamos en un request HTTP (ej. hilo en background)
        }

        return Optional.of("CRON_INTERNO");
    }
}
