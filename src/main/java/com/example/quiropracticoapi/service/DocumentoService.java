package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.DocumentoDto;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoService {
    /**
     * Inicia la Saga de subida:
     * 1. Valida el archivo (size, mime).
     * 2. Crea registro PENDIENTE en BD.
     * 3. Sube a R2.
     * 4. Actualiza a ACTIVO o ERROR_SUBIDA.
     */
    DocumentoDto subirDocumento(Integer idCliente, MultipartFile file, TipoDocumento tipo);

    /**
     * Lista los documentos de un cliente con URLs prefirmadas frescas.
     */
    List<DocumentoDto> listarDocumentosCliente(Integer idCliente);

    /**
     * Borrado lógico del documento.
     */
    void eliminarDocumento(Integer idDocumento);

    /** Genera la URL prefirmada justo cuando se va a visualizar (JIT) */
    String obtenerUrlTemporal(Integer idDocumento);
}
