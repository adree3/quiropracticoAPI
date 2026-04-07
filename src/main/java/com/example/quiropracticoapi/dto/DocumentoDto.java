package com.example.quiropracticoapi.dto;

import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentoDto {
    private Integer idDocumento;
    private Integer idCliente;
    private Integer idCita;
    private Integer idPago;
    private String notasMedicas;
    private String nombreOriginal;
    private String url;
    private TipoDocumento tipoDocumento;
    private String mimeType;
    private EstadoSubida estadoSubida;
    private Long tamanyoBytes;
    private LocalDateTime fechaSubida;
    private LocalDateTime fechaEliminacionLogica;
}
