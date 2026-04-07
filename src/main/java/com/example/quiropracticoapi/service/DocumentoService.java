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
    DocumentoDto subirDocumento(Integer idCliente, MultipartFile file, TipoDocumento tipo, Integer idCita, Integer idPago, String notas);

    /**
     * Lista los documentos de un cliente con URLs prefirmadas frescas.
     */
    List<DocumentoDto> listarDocumentosCliente(Integer idCliente);

    /**
     * Lista los documentos inactivos de un cliente (para la Papelera).
     */
    List<DocumentoDto> listarDocumentosEliminadosCliente(Integer idCliente);

    /**
     * Borrado lógico del documento.
     */
    void eliminarDocumento(Integer idDocumento);

    /**
     * Restaura un documento eliminado lógicamente devolviéndole su estado activo.
     */
    DocumentoDto restaurarDocumento(Integer idDocumento);

    /** Genera la URL prefirmada justo cuando se va a visualizar (JIT) */
    String obtenerUrlTemporal(Integer idDocumento, boolean download);

    /** Resuelve el archivo original en R2, le inyecta SUFIJO y devuelve el ByteStream puro. */
    byte[] obtenerThumbnailBytes(Integer idDocumento);

    /** Permite editar notas y asociar cita (si no tiene una ya). */
    DocumentoDto actualizarMetadatos(Integer idDocumento, Integer idCita, Integer idPago, String notas);

    /** Recupera los metadatos de un documento específico. */
    DocumentoDto obtenerDocumento(Integer idDocumento);

    /** Obtiene el stream de bytes original del archivo (Proxy). */
    byte[] obtenerArchivoPuro(Integer idDocumento);
}
