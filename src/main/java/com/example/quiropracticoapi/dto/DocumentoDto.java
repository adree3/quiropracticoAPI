package com.example.quiropracticoapi.dto;

import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentoDto {
    private Integer idDocumento;
    private String nombreOriginal;
    private String url; // URL prefirmada temporal
    private TipoDocumento tipoDocumento;
    private String mimeType;
    private EstadoSubida estadoSubida;
    private Long tamanyoBytes;
    private LocalDateTime fechaSubida;
}
