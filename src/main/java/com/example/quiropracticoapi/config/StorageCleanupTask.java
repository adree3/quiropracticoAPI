package com.example.quiropracticoapi.config;

import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.repository.DocumentoClienteRepository;
import com.example.quiropracticoapi.service.StorageService;
import com.example.quiropracticoapi.service.impl.AuditoriaServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
public class StorageCleanupTask {

    private final DocumentoClienteRepository documentoRepository;
    private final AuditoriaServiceImpl auditoriaService;
    private final StorageService storageService;

    @Autowired
    public StorageCleanupTask(DocumentoClienteRepository documentoRepository, AuditoriaServiceImpl auditoriaService, StorageService storageService) {
        this.documentoRepository = documentoRepository;
        this.auditoriaService = auditoriaService;
        this.storageService = storageService;
    }

    /**
     * Revisa registros que se quedaron en PENDIENTE por más de 30 minutos.
     * Estos suelen ser subidas interrumpidas por cierre del navegador, 
     * caída del servidor o timeouts de red.
     * 
     * Se ejecutan cada hora.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void limpiarSubidasExpiradas() {
        log.info("Iniciando limpieza de subidas de almacenamiento expiradas...");
        
        LocalDateTime umbral = LocalDateTime.now().minusMinutes(30);
        List<DocumentoCliente> pendientes = documentoRepository.findRegistrosPendientesAntiguos(umbral);
        
        if (!pendientes.isEmpty()) {
            log.warn("Se han encontrado {} registros PENDIENTE expirados. Marcando como ERROR_SUBIDA.", 
                    pendientes.size());
            
            for (DocumentoCliente doc : pendientes) {
                // Verificamos si realmente el archivo llegó a R2 (Rescate)
                if (doc.getPathArchivo() != null && storageService.exists(doc.getPathArchivo())) {
                    doc.setEstadoSubida(EstadoSubida.ACTIVO);
                    log.info("Saga Rescatada: El archivo para el documento {} existe en R2. Marcando como ACTIVO.", 
                            doc.getIdDocumento());
                    
                    auditoriaService.registrarAccion(
                        com.example.quiropracticoapi.model.enums.TipoAccion.REACTIVAR, 
                        "SISTEMA_STORAGE", 
                        doc.getIdDocumento().toString(), 
                        "Rescate de subida exitosa (Sync DB)", 
                        "SISTEMA", 
                        "El archivo existía pero la DB no se actualizó"
                    );
                } else {
                    // Fallo real o incompleto
                    doc.setEstadoSubida(EstadoSubida.ERROR_SUBIDA);
                    String errorMsg = "Subida expirada: El servidor no recibió confirmación de éxito en 30 minutos.";
                    doc.setErrorDescripcion(errorMsg);
                    
                    auditoriaService.registrarAccion(
                        com.example.quiropracticoapi.model.enums.TipoAccion.ERROR, 
                        "SISTEMA_STORAGE", 
                        doc.getIdDocumento().toString(), 
                        "Expiración de subida para cliente #" + doc.getCliente().getIdCliente(), 
                        "SISTEMA", 
                        "Fallo de subida confirmado: " + doc.getNombreOriginal()
                    );
                }
            }
            
            documentoRepository.saveAll(pendientes);
        }
        
        log.info("Limpieza completada.");
    }
}
