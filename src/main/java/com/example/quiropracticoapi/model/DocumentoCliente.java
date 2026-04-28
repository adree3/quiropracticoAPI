package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_subida", nullable = false, updatable = false))
@Table(name = "documentos_cliente", indexes = {
    @Index(name = "idx_doc_cliente", columnList = "id_cliente"),
    @Index(name = "idx_doc_estado", columnList = "estado_subida"),
    @Index(name = "idx_doc_tipo", columnList = "tipo_documento")
})
@Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class DocumentoCliente extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    /** Nombre original del archivo para mostrar en la UI. Ej: "placa_espalda.jpg" */
    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    /** Ruta exacta en R2. Nullable durante la fase PENDIENTE de la Saga. */
    @Column(name = "path_archivo", length = 500)
    private String pathArchivo;

    /** Relación opcional con la Cita donde se generó el documento clínico */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = true)
    private Cita cita;

    /** Relación opcional con el Pago asociado (para carpeta Facturación) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = true)
    private Pago pago;

    /** Interpretación médica detallada (Hallazgos, prescripciones asociadas) */
    @Column(name = "notas_medicas", columnDefinition = "TEXT")
    private String notasMedicas;

    /** Tipo de negocio del documento */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumento tipoDocumento;

    /** MIME type real verificado por Apache Tika. Ej: "application/pdf", "image/jpeg" */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /** Estado de la Saga de subida */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_subida", nullable = false, length = 20)
    private EstadoSubida estadoSubida = EstadoSubida.PENDIENTE;

    /** Solo se rellena si estadoSubida = ERROR_SUBIDA. Permite al admin diagnosticar el fallo. */
    @Column(name = "error_descripcion", length = 500)
    private String errorDescripcion;

    /** Tamaño real del archivo en bytes. Para mostrar en UI (ej: "4,3 MB") y controlar cuota. */
    @Column(name = "tamanyo_bytes", nullable = false)
    private Long tamanyoBytes;

    /** Borrado lógico. Los archivos médicos NUNCA se eliminan físicamente de R2
     *  salvo petición expresa por derecho al olvido (RGPD). */
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** Fecha en la que el documento fue enviado a la papelera (borrado lógico) */
    @Column(name = "fecha_eliminacion_logica")
    private LocalDateTime fechaEliminacionLogica;
}
