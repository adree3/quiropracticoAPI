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

    @Column(name = "cif_nif", length = 20)
    private String cifNif;

    @Column(length = 20)
    private String telefono;

    @Column(name = "email_contacto", length = 100)
    private String emailContacto;

    @Column(length = 255)
    private String direccion;

    @Column(name = "duracion_cita_minutos", nullable = false)
    @Builder.Default
    private Integer duracionCitaMinutos = 30;

    @Column(name = "limite_almacenamiento_bytes", nullable = false)
    @Builder.Default
    private Long limiteAlmacenamientoBytes = 5368709120L;

    @Column(name = "almacenamiento_usado_bytes", nullable = false)
    @Builder.Default
    private Long almacenamientoUsadoBytes = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "whatsapp_phone_number_id", length = 100)
    private String whatsappPhoneNumberId;

    @Column(name = "whatsapp_access_token", columnDefinition = "TEXT")
    private String whatsappAccessToken;

    @Column(name = "telefono_atencion_humana", length = 30)
    private String telefonoAtencionHumana;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "usar_dos_telefonos_whatsapp", nullable = false)
    @Builder.Default
    private boolean usarDosTelefonosWhatsapp = false;

    @Override
    public boolean isEliminadoLogico() {
        return !activa;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Clínica #%d | %s | Activa: %s", idClinica, nombre, activa ? "Sí" : "No");
    }
}
