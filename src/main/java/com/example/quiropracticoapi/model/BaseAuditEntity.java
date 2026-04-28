package com.example.quiropracticoapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Clase base para centralizar los campos de auditoría técnica.
 * Proporciona fecha de creación y última modificación de forma automática.
 */
@MappedSuperclass
@Getter
@Setter
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "clinicaId", type = Long.class))
public abstract class BaseAuditEntity {

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        
        Long tenantId = com.example.quiropracticoapi.config.TenantContext.getTenantId();
        if (tenantId != null) {
            try {
                java.lang.reflect.Field field = getClinicaField(this.getClass());
                if (field != null) {
                    field.setAccessible(true);
                    if (field.get(this) == null) {
                        Clinica ref = new Clinica();
                        ref.setIdClinica(tenantId);
                        field.set(this, ref);
                    }
                }
            } catch (Exception e) {
                // Silencioso si la entidad no tiene el campo clinica
            }
        }
    }

    private java.lang.reflect.Field getClinicaField(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField("clinica");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    @PreUpdate
    protected void onUpdate() {
        this.ultimaModificacion = LocalDateTime.now();
    }
}
