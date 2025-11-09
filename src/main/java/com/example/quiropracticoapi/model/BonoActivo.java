package com.example.quiropracticoapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bonos_activos")
public class BonoActivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bono_activo")
    private Integer idBonoActivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio_comprado", nullable = false)
    private Servicio servicioComprado;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pago_origen", nullable = false, unique = true)
    private Pago pagoOrigen;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "sesiones_totales", nullable = false)
    private int sesionesTotales;

    @Column(name = "sesiones_restantes", nullable = false)
    private int sesionesRestantes;

    @Column(name = "fecha_caducidad")
    private LocalDate fechaCaducidad;
}
