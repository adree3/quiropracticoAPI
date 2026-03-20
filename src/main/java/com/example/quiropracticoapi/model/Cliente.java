package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.Auditable;
import jakarta.persistence.*;
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
@Table(name = "clientes", indexes = {
    @Index(name = "idx_cliente_activo", columnList = "activo")
})
public class Cliente implements SoftDeletable, Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", nullable = false, length = 25)
    private String telefono;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "notas_privadas", columnDefinition = "TEXT")
    private String notasPrivadas;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** Ruta en R2 de la foto de perfil. Nullable si no tiene foto. 
     *  Ej: "clientes/15/perfil/foto_perfil.jpg" */
    @Column(name = "foto_perfil_path", length = 500)
    private String fotoPerfilPath;

    @Override
    public boolean isEliminadoLogico() {
        return !this.activo;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Cliente #%s | %s %s | Activo: %s",
                idCliente, nombre, apellidos, activo ? "Sí" : "No");
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
    }
}
