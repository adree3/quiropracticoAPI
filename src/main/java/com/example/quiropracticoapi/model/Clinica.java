package com.example.quiropracticoapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "clinicas")
public class Clinica extends BaseAuditEntity implements SoftDeletable, Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idClinica;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private boolean activa = true;

    @Override
    public boolean isEliminadoLogico() {
        return !activa;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Clínica #%d | %s | Activa: %s", idClinica, nombre, activa ? "Sí" : "No");
    }
}
