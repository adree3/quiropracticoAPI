package com.example.quiropracticoapi.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Intercepta la ejecución de cualquier método en los repositorios o servicios.
     * De esta forma garantizamos que el filtro de Hibernate se active dentro del 
     * contexto transaccional (la Session activa) que usa Spring Data JPA.
     */
    @Pointcut("execution(* com.example.quiropracticoapi.repository.*.*(..)) && " +
              "!execution(* com.example.quiropracticoapi.repository.UsuarioRepository.*(..))")
    public void tenantFilterPointcut() {
        // Pointcut vacío, solo define dónde se aplica el aspecto.
    }

    @Before("tenantFilterPointcut()")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getTenantId();
        
        // Si hay un tenant configurado (y no es 0, el cual reservamos para super_admin global)
        if (tenantId != null && tenantId > 0L) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("clinicaId", tenantId);
        }
    }
}
