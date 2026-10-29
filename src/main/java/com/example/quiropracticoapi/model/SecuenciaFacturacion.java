package com.example.quiropracticoapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "secuencias_facturacion", uniqueConstraints = {
    @UniqueConstraint(name = "uk_secuencias_clinica_prefijo", columnNames = {"clinica_id", "prefijo_anio"})
})
@Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class SecuenciaFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Column(name = "prefijo_anio", nullable = false, length = 10)
    private String prefijoAnio;

    @Column(name = "ultimo_numero", nullable = false)
    private Integer ultimoNumero = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
