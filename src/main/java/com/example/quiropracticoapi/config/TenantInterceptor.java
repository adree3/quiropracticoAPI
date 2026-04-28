package com.example.quiropracticoapi.config;

import com.example.quiropracticoapi.service.impl.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true;
        }

        try {
            String jwt = authHeader.substring(7);
            Long clinicaId = jwtService.extractClinicaId(jwt);

            // Si no hay clinicaId en el token o es 0 (super_admin en modo global) → sin filtro
            if (clinicaId == null || clinicaId == 0L) {
                return true;
            }

            // Guardar en ThreadLocal para usarlo en los @PrePersist de JPA (INSERTs)
            // y en el TenantFilterAspect (Lecturas/Transacciones)
            TenantContext.setTenantId(clinicaId);

        } catch (Exception e) {
            log.warn("Error extrayendo clinicaId del JWT para tenant filter: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Limpiar ThreadLocal al finalizar la petición para evitar fugas de memoria y cruces de sesiones
        TenantContext.clear();
    }
}
