package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.DocumentoDto;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import com.example.quiropracticoapi.service.DocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@Tag(name = "Documentos", description = "Gestión de archivos médicos y fotos de perfil en Cloudflare R2")
public class DocumentoController {

    private final DocumentoService documentoService;

    @Autowired
    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Operation(summary = "Subir documento a un cliente", description = "Inicia la saga de subida a R2 con validación MIME. Permite vinculación a Cita.")
    @PostMapping("/clientes/{idCliente}")
    public ResponseEntity<DocumentoDto> subirDocumento(
            @PathVariable Integer idCliente,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam(value = "idCita", required = false) Integer idCita,
            @RequestParam(value = "idPago", required = false) Integer idPago,
            @RequestParam(value = "notas", required = false) String notas) {
        return ResponseEntity.ok(documentoService.subirDocumento(idCliente, file, tipo, idCita, idPago, notas));
    }

    @Operation(summary = "Listar documentos de un cliente", description = "Devuelve metadatos y URLs prefirmadas temporales (15 min).")
    @GetMapping("/clientes/{idCliente}")
    public ResponseEntity<List<DocumentoDto>> listarDocumentos(@PathVariable Integer idCliente) {
        return ResponseEntity.ok(documentoService.listarDocumentosCliente(idCliente));
    }

    @Operation(summary = "Listar documentos eliminados", description = "Devuelve la bandeja de la papelera de un cliente.")
    @GetMapping("/clientes/{idCliente}/papelera")
    public ResponseEntity<List<DocumentoDto>> listarDocumentosEliminados(@PathVariable Integer idCliente) {
        return ResponseEntity.ok(documentoService.listarDocumentosEliminadosCliente(idCliente));
    }

    @Operation(summary = "Borrado lógico de documento")
    @DeleteMapping("/{idDocumento}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Integer idDocumento) {
        documentoService.eliminarDocumento(idDocumento);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener URL temporal (JIT)", description = "Genera una URL prefirmada de 15 min justo antes de visualizar el archivo.")
    @GetMapping("/{idDocumento}/url")
    public ResponseEntity<String> obtenerUrl(
            @PathVariable Integer idDocumento,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        return ResponseEntity.ok(documentoService.obtenerUrlTemporal(idDocumento, download));
    }

    @Operation(summary = "Descargar documento (Proxy)", description = "Descarga el archivo directamente a través del servidor para evitar bloqueos de CORS.")
    @GetMapping("/{idDocumento}/download")
    public ResponseEntity<org.springframework.core.io.Resource> descargarDocumento(@PathVariable Integer idDocumento) {
        DocumentoDto doc = documentoService.obtenerDocumento(idDocumento);
        byte[] bytes = documentoService.obtenerArchivoPuro(idDocumento);
        
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNombreOriginal() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(doc.getMimeType()))
                .contentLength(bytes.length)
                .body(resource);
    }

    @Operation(summary = "Visualizar documento inline (Proxy)", description = "Abre el archivo directamente en el navegador (sin descargar) para PDFs e imágenes.")
    @GetMapping("/{idDocumento}/view")
    public ResponseEntity<org.springframework.core.io.Resource> visualizarDocumento(@PathVariable Integer idDocumento) {
        DocumentoDto doc = documentoService.obtenerDocumento(idDocumento);
        byte[] bytes = documentoService.obtenerArchivoPuro(idDocumento);

        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombreOriginal() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(doc.getMimeType()))
                .contentLength(bytes.length)
                .body(resource);
    }
    @GetMapping("/{idDocumento}/thumbnail")
    public ResponseEntity<byte[]> obtenerThumbnail(@PathVariable Integer idDocumento) {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(documentoService.obtenerThumbnailBytes(idDocumento));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DocumentoDto> actualizarMetadatos(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer idCita,
            @RequestParam(required = false) Integer idPago,
            @RequestParam(required = false) String notas) {
        return ResponseEntity.ok(documentoService.actualizarMetadatos(id, idCita, idPago, notas));
    }

    @Operation(summary = "Restaurar documento", description = "Devuelve un documento al estado activo.")
    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<DocumentoDto> restaurarDocumento(@PathVariable Integer id) {
        return ResponseEntity.ok(documentoService.restaurarDocumento(id));
    }
}
