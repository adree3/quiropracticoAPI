package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.DocumentoDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.exception.StorageException;
import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.Pago;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.DocumentoClienteRepository;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.config.TenantContext;
import com.example.quiropracticoapi.repository.PagoRepository;
import com.example.quiropracticoapi.service.DocumentoService;
import com.example.quiropracticoapi.service.StorageService;
import com.example.quiropracticoapi.util.StoragePathBuilder;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoServiceImpl.class);

    private final DocumentoClienteRepository documentoRepository;
    private final ClienteRepository clienteRepository;
    private final CitaRepository citaRepository;
    private final PagoRepository pagoRepository;
    private final StorageService storageService;
    private final AuditoriaServiceImpl auditoriaService;
    private final StoragePathBuilder storagePathBuilder;
    private final Tika tika = new Tika();

    @Autowired
    public DocumentoServiceImpl(DocumentoClienteRepository documentoRepository,
                                ClienteRepository clienteRepository,
                                CitaRepository citaRepository,
                                PagoRepository pagoRepository,
                                StorageService storageService,
                                AuditoriaServiceImpl auditoriaService,
                                StoragePathBuilder storagePathBuilder) {
        this.documentoRepository = documentoRepository;
        this.clienteRepository = clienteRepository;
        this.citaRepository = citaRepository;
        this.pagoRepository = pagoRepository;
        this.storageService = storageService;
        this.auditoriaService = auditoriaService;
        this.storagePathBuilder = storagePathBuilder;
    }

    @Override
    @Transactional
    public DocumentoDto subirDocumento(Integer idCliente, MultipartFile file, TipoDocumento tipo, Integer idCita, Integer idPago, String notas) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Cita cita = null;
        if (idCita != null) {
            cita = citaRepository.findById(idCita)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        }

        Pago pago = null;
        if (idPago != null) {
            pago = pagoRepository.findById(idPago)
                    .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        }

        // Validación de unicidad: una cita solo puede tener 1 firma activa vinculada
        if (tipo == TipoDocumento.JUSTIFICANTE_ASISTENCIA && idCita != null) {
            boolean yaExisteFirma = documentoRepository.existsFirmaActivaParaCita(
                    idCita, TipoDocumento.JUSTIFICANTE_ASISTENCIA);
            if (yaExisteFirma) {
                throw new IllegalStateException(
                    "La cita #" + idCita + " ya tiene una cita firmada vinculada activa. " +
                    "Elimina la firma existente antes de subir una nueva.");
            }
        }

        // 1. Validación de seguridad (MIME Type real)
        String detectedMimeType = detectSafeMimeType(file);
        
        // 2. Crear registro PENDIENTE en BD (Inicio Saga)
        DocumentoCliente doc = new DocumentoCliente();
        doc.setCliente(cliente);
        doc.setCita(cita);
        doc.setPago(pago);
        doc.setNotasMedicas(notas);
        doc.setNombreOriginal(file.getOriginalFilename());
        doc.setTipoDocumento(tipo);
        doc.setMimeType(detectedMimeType);
        doc.setTamanyoBytes(file.getSize());
        doc.setEstadoSubida(EstadoSubida.ACTIVO); // Directo a activo para simplificar si no hay saga asíncrona real
        doc.setFechaCreacion(LocalDateTime.now());
        
        DocumentoCliente docGuardado = documentoRepository.save(doc);

        // 3. Definir Path Dinámico R2 (Protección GDPR e Historización Contable)
        String extension = getExtension(file.getOriginalFilename());
        String path;
        Long clinicaId = TenantContext.getTenantId();

        if (tipo == TipoDocumento.JUSTIFICANTE_PAGO) {
            path = storagePathBuilder.buildFacturacionPath(clinicaId, idCliente, docGuardado.getIdDocumento(), extension, docGuardado.getFechaCreacion());
        } else {
            // Nomenclatura semántica específica
            String subject;
            if (tipo == TipoDocumento.RADIOGRAFIA || tipo == TipoDocumento.RESONANCIA) {
                subject = "imagen";
            } else if (tipo == TipoDocumento.JUSTIFICANTE_ASISTENCIA) {
                subject = "cita_firmada";
            } else if (tipo == TipoDocumento.JUSTIFICANTE_PAGO) {
                subject = (pago != null && pago.getServicioPagado() != null) 
                        ? "cobro_" + pago.getServicioPagado().getNombreServicio().toLowerCase().replace(" ", "_")
                        : "cobro";
            } else if (tipo == TipoDocumento.CONSENTIMIENTO_LOPD || tipo == TipoDocumento.CONSENTIMIENTO_TRATAMIENTO) {
                subject = "consentimiento_" + tipo.name().replace("CONSENTIMIENTO_", "").toLowerCase();
            } else if (tipo == TipoDocumento.INFORME_MEDICO) {
                subject = "informe";
            } else {
                subject = "documento";
            }

            String baseName;
            if (idCita != null) {
                baseName = String.format("cita_%d_%s_%d", idCita, subject, docGuardado.getIdDocumento());
            } else if (idPago != null) {
                baseName = String.format("pago_%d_%s_%d", idPago, subject, docGuardado.getIdDocumento());
            } else {
                baseName = String.format("%s_%d", subject, docGuardado.getIdDocumento());
            }
            
            if (tipo == TipoDocumento.RADIOGRAFIA || tipo == TipoDocumento.RESONANCIA) {
                path = storagePathBuilder.buildPruebasMedicasPath(clinicaId, idCliente, baseName, extension);
            } else if (tipo == TipoDocumento.CONSENTIMIENTO_LOPD || tipo == TipoDocumento.CONSENTIMIENTO_TRATAMIENTO) {
                path = storagePathBuilder.buildConsentimientosPath(clinicaId, idCliente, baseName, extension);
            } else if (tipo == TipoDocumento.INFORME_MEDICO) {
                path = storagePathBuilder.buildInformesPath(clinicaId, idCliente, baseName, extension);
            } else {
                path = storagePathBuilder.buildDocumentosGenericosPath(clinicaId, idCliente, baseName, extension);
            }
            
            // Actualizar el nombre que verá el usuario en Flutter para ser descriptivo
            docGuardado.setNombreOriginal(baseName + extension);
        }

        try {
            // 4. Procesamiento dual (Miniatura en RAM) ANTES de consumir el stream principal
            if (detectedMimeType != null && detectedMimeType.startsWith("image/")) {
                String thumbPath = getThumbPath(path);
                try {
                    java.io.ByteArrayOutputStream thumbOs = new java.io.ByteArrayOutputStream();
                    net.coobird.thumbnailator.Thumbnails.of(file.getInputStream())
                            .size(600, 600)
                            .outputFormat("jpg")
                            .outputQuality(0.9)
                            .toOutputStream(thumbOs);
                    
                    storageService.storeBytes(thumbOs.toByteArray(), thumbPath, "image/jpeg");
                } catch (Exception e) {
                    log.error("Error generando miniatura para el archivo {}. Ejecutando Fallback de seguridad.", file.getOriginalFilename(), e);
                    // Fallback: Si ImageIO falla (ej. .webp u otro formato no soportado/corrupto), subimos el original como miniatura
                    storageService.storeBytes(file.getBytes(), thumbPath, file.getContentType());
                }
            }
            
            // Subida del archivo principal (Ahora sí, consumimos el stream final)
            storageService.store(file, path);
            
            // 5. Éxito: Actualizar a ACTIVO
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
        return documentoRepository.findByClienteIdClienteAndActivoTrueOrderByFechaCreacionDesc(idCliente)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentoDto> listarDocumentosCita(Integer idCita) {
        return documentoRepository.findByCitaIdCitaAndActivoTrueOrderByFechaCreacionDesc(idCita)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentoDto> listarDocumentosEliminadosCliente(Integer idCliente) {
        return documentoRepository.findByClienteIdClienteAndActivoFalseOrderByFechaEliminacionLogicaDesc(idCliente)
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
        doc.setFechaEliminacionLogica(LocalDateTime.now());
        documentoRepository.save(doc);
        
        // Comentario de Compliance RGPD: Se hace SOLO borrado lógico. 
        // No se llama a r2StorageService.delete(...) para mantener registro histórico.
        
        auditoriaService.registrarAccion(TipoAccion.ELIMINAR_LOGICO, "DOCUMENTO", 
                idDocumento.toString(), "Documento marcado como inactivo: " + doc.getNombreOriginal());
    }

    @Override
    @Transactional
    public DocumentoDto restaurarDocumento(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        
        doc.setActivo(true);
        doc.setFechaEliminacionLogica(null);
        documentoRepository.save(doc);
        
        auditoriaService.registrarAccion(TipoAccion.REACTIVAR, "DOCUMENTO",
                idDocumento.toString(), "Documento restaurado de la papelera: " + doc.getNombreOriginal());
        
        return mapToDto(doc);
    }


    private String detectSafeMimeType(MultipartFile file) {
        try {
            // Tika inspecciona los "Magic Numbers" del archivo, no solo la extensión
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            return file.getContentType();
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getThumbPath(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) return path + "_thumb.jpg";
        return path.substring(0, dotIndex) + "_thumb.jpg";
    }

    @Override
    public String obtenerUrlTemporal(Integer idDocumento, boolean download) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (doc.getEstadoSubida() != EstadoSubida.ACTIVO || doc.getPathArchivo() == null) {
            throw new IllegalStateException("El documento no está listo para ser visualizado.");
        }

        if (download) {
            return storageService.generatePresignedUrl(doc.getPathArchivo(), doc.getNombreOriginal());
        }
        return storageService.generatePresignedUrl(doc.getPathArchivo());
    }

    @Override
    public String obtenerUrlThumbnail(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (doc.getEstadoSubida() != EstadoSubida.ACTIVO || doc.getPathArchivo() == null) {
            throw new IllegalStateException("El documento no está listo para ser visualizado.");
        }
        
        String thumbPath = getThumbPath(doc.getPathArchivo());
        // Intentar firmar la miniatura, si falla o no existe (ej. legacy), firmar el original
        try {
            return storageService.generatePresignedUrl(thumbPath);
        } catch (Exception e) {
            return storageService.generatePresignedUrl(doc.getPathArchivo());
        }
    }

    @Override
    @Transactional
    public DocumentoDto actualizarMetadatos(Integer idDocumento, Integer idCita, Integer idPago, String notas) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        // Regla de negocio: Si ya tiene cita, no se puede cambiar (para mantener integridad de nombres/R2)
        if (idCita != null && doc.getCita() == null) {
            Cita cita = citaRepository.findById(idCita)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
            doc.setCita(cita);
            
            // Actualizamos el nombre original para que sea descriptivo en la UI
            String extension = getExtension(doc.getNombreOriginal());
            doc.setNombreOriginal("cita_" + idCita + "_documento_" + idDocumento + extension);
        }

        // Regla de negocio: Vinculación a Pago si no tiene uno asociado
        if (idPago != null && doc.getPago() == null) {
            Pago pago = pagoRepository.findById(idPago)
                    .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
            doc.setPago(pago);
            
            String extension = getExtension(doc.getNombreOriginal());
            doc.setNombreOriginal("pago_" + idPago + "_documento_" + idDocumento + extension);
        }

        if (notas != null) {
            doc.setNotasMedicas(notas);
        }

        return mapToDto(documentoRepository.save(doc));
    }

    @Override
    public DocumentoDto obtenerDocumento(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        return mapToDto(doc);
    }

    @Override
    public byte[] obtenerArchivoPuro(Integer idDocumento) {
        DocumentoCliente doc = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        
        if (doc.getPathArchivo() == null) {
            throw new StorageException("El archivo no tiene un path asociado");
        }
        
        return storageService.getFileBytes(doc.getPathArchivo());
    }

    private DocumentoDto mapToDto(DocumentoCliente doc) {
        DocumentoDto dto = new DocumentoDto();
        dto.setIdDocumento(doc.getIdDocumento());
        dto.setIdCliente(doc.getCliente().getIdCliente());
        if (doc.getCita() != null) {
            dto.setIdCita(doc.getCita().getIdCita());
        }
        if (doc.getPago() != null) {
            dto.setIdPago(doc.getPago().getIdPago());
        }
        dto.setNotasMedicas(doc.getNotasMedicas());
        dto.setNombreOriginal(doc.getNombreOriginal());
        dto.setTipoDocumento(doc.getTipoDocumento());
        dto.setMimeType(doc.getMimeType());
        dto.setEstadoSubida(doc.getEstadoSubida());
        dto.setTamanyoBytes(doc.getTamanyoBytes());
        dto.setFechaSubida(doc.getFechaCreacion());
        dto.setFechaEliminacionLogica(doc.getFechaEliminacionLogica());
        
        // Inyectamos la miniatura para el GridView (Zero-Copy)
        if (doc.getMimeType() != null && doc.getMimeType().startsWith("image/")) {
            try {
                dto.setThumbnailUrl(obtenerUrlThumbnail(doc.getIdDocumento()));
            } catch (Exception e) {
                // Si falla la firma, no bloqueamos el listado
            }
        }
        
        return dto;
    }
}
