package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.enums.EstadoFactura;
import com.example.quiropracticoapi.model.enums.TipoFactura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "facturas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_facturas_clinica_numero", columnNames = {"clinica_id", "numero_factura"})
})
@Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class Factura extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "base_imponible", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_factura", nullable = false, length = 20)
    private TipoFactura tipoFactura;

    @Column(name = "ruta_pdf_r2", length = 500)
    private String rutaPdfR2;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.EMITIDA;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaFactura> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "factura")
    private List<Pago> pagosAsociados = new ArrayList<>();
}
