package com.example.quiropracticoapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "consumos_bono")
public class ConsumoBono {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo")
    private Integer idConsumo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_bono_activo", nullable = false)
    private BonoActivo bonoActivo;

    @Column(name = "fecha_consumo", nullable = false, updatable = false)
    private LocalDateTime fechaConsumo;

    @Column(name = "sesiones_restantes_snapshot")
    private Integer sesionesRestantesSnapshot;

    @PrePersist
    protected void onCreate() {
        if (this.fechaConsumo == null) {
            this.fechaConsumo = LocalDateTime.now();
        }
    }
}
