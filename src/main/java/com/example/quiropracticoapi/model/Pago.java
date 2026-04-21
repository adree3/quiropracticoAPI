package com.example.quiropracticoapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.quiropracticoapi.model.enums.MetodoPago;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_pago", nullable = false, updatable = false))
@Table(name = "pagos")
public class Pago extends BaseAuditEntity implements Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio_pagado", nullable = true)
    private Servicio servicioPagado;

    @Column(name = "notas", length = 255)
    private String notas;

    @Column(name = "pagado", nullable = false)
    private boolean pagado = true;

    @Override
    public String toResumen(TipoAccion accion) {
        return String.format("Pago #%s | Cliente: %s | Monto: %s€ | Método: %s",
                idPago, 
                (cliente != null ? cliente.getNombre() + " " + cliente.getApellidos() : "N/A"), 
                monto, 
                metodoPago);
    }
}
