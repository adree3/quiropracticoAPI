package com.example.quiropracticoapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Clase base para centralizar los campos de auditoría técnica.
 */
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "clinicaId", type = Long.class))
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;

    @CreatedBy
    @Column(name = "creado_por", updatable = false, length = 100)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "modificado_por", length = 100)
    private String modificadoPor;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Métodos para la asignación de tenant si corresponde, el EntityListener se encarga del resto
    @jakarta.persistence.PrePersist
    protected void onPrePersistTenant() {
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
}
