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

    @Operation(summary = "Subir documento a un cliente", description = "Inicia la saga de subida a R2 con validación MIME.")
    @PostMapping("/clientes/{idCliente}")
    public ResponseEntity<DocumentoDto> subirDocumento(
            @PathVariable Integer idCliente,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") TipoDocumento tipo) {
        return ResponseEntity.ok(documentoService.subirDocumento(idCliente, file, tipo));
    }

    @Operation(summary = "Listar documentos de un cliente", description = "Devuelve metadatos y URLs prefirmadas temporales (15 min).")
    @GetMapping("/clientes/{idCliente}")
    public ResponseEntity<List<DocumentoDto>> listarDocumentos(@PathVariable Integer idCliente) {
        return ResponseEntity.ok(documentoService.listarDocumentosCliente(idCliente));
    }

    @Operation(summary = "Borrado lógico de documento")
    @DeleteMapping("/{idDocumento}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Integer idDocumento) {
        documentoService.eliminarDocumento(idDocumento);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener URL temporal (JIT)", description = "Genera una URL prefirmada de 15 min justo antes de visualizar el archivo.")
    @GetMapping("/{idDocumento}/url")
    public ResponseEntity<String> obtenerUrl(@PathVariable Integer idDocumento) {
        return ResponseEntity.ok(documentoService.obtenerUrlTemporal(idDocumento));
    }
}
