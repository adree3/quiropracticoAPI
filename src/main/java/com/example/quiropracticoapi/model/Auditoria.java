package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.enums.TipoAccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAuditoria;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_usuario_responsable")
    private Integer idUsuarioResponsable;

    @Column(name = "username_responsable")
    private String usernameResponsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAccion accion;

    // Sobre quien lo hizo (uuario, cita...)
    @Column(nullable = false)
    private String entidad;

    // Id del objeto afectado
    @Column(name = "id_entidad")
    private String idEntidad;

    // Detalles extra
    @Column(columnDefinition = "TEXT")
    private String detalles;
}