package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.DocumentoDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.exception.StorageException;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.DocumentoClienteRepository;
import com.example.quiropracticoapi.service.DocumentoService;
import com.example.quiropracticoapi.service.StorageService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoClienteRepository documentoRepository;
    private final ClienteRepository clienteRepository;
    private final StorageService storageService;
    private final AuditoriaServiceImpl auditoriaService;
    private final Tika tika = new Tika();

    @Autowired
    public DocumentoServiceImpl(DocumentoClienteRepository documentoRepository,
                                ClienteRepository clienteRepository,
                                StorageService storageService,
                                AuditoriaServiceImpl auditoriaService) {
        this.documentoRepository = documentoRepository;
        this.clienteRepository = clienteRepository;
        this.storageService = storageService;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public DocumentoDto subirDocumento(Integer idCliente, MultipartFile file, TipoDocumento tipo) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        // 1. Validación de seguridad (MIME Type real)
        String detectedMimeType = detectSafeMimeType(file);
        
        // 2. Crear registro PENDIENTE en BD (Inicio Saga)
        DocumentoCliente doc = new DocumentoCliente();
        doc.setCliente(cliente);
        doc.setNombreOriginal(file.getOriginalFilename());
        doc.setTipoDocumento(tipo);
        doc.setMimeType(detectedMimeType);
        doc.setTamanyoBytes(file.getSize());
        doc.setEstadoSubida(EstadoSubida.PENDIENTE);
        doc.setFechaSubida(LocalDateTime.now());
        
        DocumentoCliente docGuardado = documentoRepository.save(doc);

        // 3. Definir Path y subir a R2
        String extension = getExtension(file.getOriginalFilename());
        String carpeta = (tipo == TipoDocumento.RADIOGRAFIA || tipo == TipoDocumento.RESONANCIA) 
                ? "pruebas_medicas" : "documentos";
        
        String path = String.format("clientes/%d/%s/%s_%d%s", 
                idCliente, carpeta, tipo.name().toLowerCase(), docGuardado.getIdDocumento(), extension);

        try {
            storageService.store(file, path);
            
            // 4. Éxito: Actualizar a ACTIVO
            docGuardado.setPathArchivo(path);
            docGuardado.setEstadoSubida(EstadoSubida.ACTIVO);
            documentoRepository.save(docGuardado);
            
            auditoriaService.registrarAccion(TipoAccion.CREAR, "DOCUMENTO", 
                    docGuardado.getIdDocumento().toString(), 
                    "Archivo subido: " + docGuardado.getNombreOriginal(), null, 
                    "Subida de " + tipo.name());

        } catch (Exception e) {
            // 4. Fallo: Registrar error para auditoría admin (Saga fallida)
            docGuardado.setEstadoSubida(EstadoSubida.ERROR_SUBIDA);
            docGuardado.setErrorDescripcion(e.getMessage());
            documentoRepository.save(docGuardado);
            
            // Log de auditoría de error
            auditoriaService.registrarAccion(
                TipoAccion.ERROR, 
                "DOCUMENTO", 
                docGuardado.getIdDocumento().toString(), 
                "Fallo subida a R2: " + e.getMessage(), 
                null, 
                "Error en subida de " + docGuardado.getNombreOriginal()
            );
            
            throw new StorageException("Error al subir el documento a la nube", e);
        }

        return mapToDto(docGuardado);
    }

    @Override
    public List<DocumentoDto> listarDocumentosCliente(Integer idCliente) {
        return documentoRepository.findByClienteIdClienteAndActivoTrueOrderByFechaSubidaDesc(idCliente)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarDocumento(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        
        doc.setActivo(false);
        documentoRepository.save(doc);
        
        auditoriaService.registrarAccion(TipoAccion.ELIMINAR_LOGICO, "DOCUMENTO", 
                idDocumento.toString(), "Documento marcado como inactivo: " + doc.getNombreOriginal());
    }


    private String detectSafeMimeType(MultipartFile file) {
        try {
            // Tika inspecciona los "Magic Numbers" del archivo, no solo la extensión
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            return file.getContentType(); // Fallback al proporcionado por el navegador
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    @Override
    public String obtenerUrlTemporal(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (doc.getEstadoSubida() != EstadoSubida.ACTIVO || doc.getPathArchivo() == null) {
            throw new IllegalStateException("El documento no está listo para ser visualizado.");
        }

        return storageService.generatePresignedUrl(doc.getPathArchivo());
    }

    private DocumentoDto mapToDto(DocumentoCliente doc) {
        DocumentoDto dto = new DocumentoDto();
        dto.setIdDocumento(doc.getIdDocumento());
        dto.setNombreOriginal(doc.getNombreOriginal());
        dto.setTipoDocumento(doc.getTipoDocumento());
        dto.setMimeType(doc.getMimeType());
        dto.setEstadoSubida(doc.getEstadoSubida());
        dto.setTamanyoBytes(doc.getTamanyoBytes());
        dto.setFechaSubida(doc.getFechaSubida());
        
        // JIT: No devolvemos URL en el mapeo general para evitar caducidad en el front
        return dto;
    }
}
