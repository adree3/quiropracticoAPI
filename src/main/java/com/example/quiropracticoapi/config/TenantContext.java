package com.example.quiropracticoapi.config;

/**
 * Contexto para almacenar el clinicaId en un ThreadLocal,
 * permitiendo acceder a él desde cualquier parte del hilo actual (ej. PrePersist, Repositorios).
 */
public class TenantContext {
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
