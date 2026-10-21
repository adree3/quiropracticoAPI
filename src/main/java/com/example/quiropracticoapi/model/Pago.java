package com.example.quiropracticoapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.quiropracticoapi.model.enums.MetodoPago;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
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
@Table(name = "pagos")
@Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class Pago extends BaseAuditEntity implements Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

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

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "cobrado_por", length = 100)
    private String cobradoPor;

    @Column(name = "anulado", nullable = false)
    private boolean anulado = false;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Override
    public String toResumen(TipoAccion accion) {
        return String.format("Pago #%s | Cliente: %s | Monto: %s€ | Método: %s",
                idPago, 
                (cliente != null ? cliente.getNombre() + " " + cliente.getApellidos() : "N/A"), 
                monto, 
                metodoPago);
    }
}
