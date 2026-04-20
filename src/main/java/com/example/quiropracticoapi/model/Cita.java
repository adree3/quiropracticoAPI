package com.example.quiropracticoapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.quiropracticoapi.model.Auditable;
import com.example.quiropracticoapi.model.enums.EstadoCita;
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
@Table(name = "citas", indexes = {
    @Index(name = "idx_cita_fechas_estado", columnList = "fecha_hora_inicio, fecha_hora_fin, estado"),
    @Index(name = "idx_cita_estado", columnList = "estado")
})
public class Cita implements SoftDeletable, Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_quiropractico", nullable = false)
    private Usuario quiropractico;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCita estado = EstadoCita.programada;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "firmada", nullable = false)
    private boolean firmada = false;

    @Column(name = "ruta_justificante")
    private String rutaJustificante;

    @Column(name = "firma_base64", columnDefinition = "MEDIUMTEXT")
    private String firmaBase64;

    @Column(name = "notas_recepcion", columnDefinition = "TEXT")
    private String notasRecepcion;

    @Column(name = "id_bono_preasignado")
    private Integer idBonoPreasignado;

    @JsonIgnore
    @OneToOne(mappedBy = "cita", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private ConsumoBono consumoBono;

    @Override
    public boolean isEliminadoLogico() {
        return EstadoCita.cancelada == this.estado;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Cita #%s | Estado: %s | Inicio: %s",
                idCita, estado, fechaHoraInicio);
    }
}
