package com.example.quiropracticoapi.model;

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
@Table(name = "clientes")
public class Cliente {
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

    @Column(name = "activo")
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
    }
}
