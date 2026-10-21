package com.example.quiropracticoapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_alta", nullable = false, updatable = false))
@Table(name = "clientes", indexes = {
    @Index(name = "idx_cliente_activo", columnList = "activo"),
    @Index(name = "idx_cliente_email_clinica", columnList = "email, clinica_id"),
    @Index(name = "idx_cliente_telefono_clinica", columnList = "telefono, clinica_id")
})
@Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class Cliente extends BaseAuditEntity implements SoftDeletable, Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "dni_nie", length = 20)
    private String dniNie;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "prefijo_telefono", length = 10, nullable = false)
    private String prefijoTelefono = "+34";

    @Column(name = "telefono", nullable = false, length = 25)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "notas_privadas", columnDefinition = "TEXT")
    private String notasPrivadas;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "lopd_firmada", nullable = false)
    private boolean lopdFirmada = false;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "baja_por", length = 100)
    private String bajaPor;

    @PrePersist
    @PreUpdate
    public void cleanTelefono() {
        if (this.telefono != null) {
            this.telefono = this.telefono.replaceAll("[\\s-]", "");
        }
    }

    @Override
    public boolean isEliminadoLogico() {
        return !this.activo;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Cliente #%s | %s %s | Activo: %s",
                idCliente, nombre, apellidos, activo ? "Sí" : "No");
    }
}
