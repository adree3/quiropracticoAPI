package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.config.TenantContext;
import com.example.quiropracticoapi.model.DocumentoCliente;
import com.example.quiropracticoapi.model.Factura;
import com.example.quiropracticoapi.model.enums.EstadoSubida;
import com.example.quiropracticoapi.model.enums.TipoDocumento;
import com.example.quiropracticoapi.repository.ClinicaRepository;
import com.example.quiropracticoapi.repository.DocumentoClienteRepository;
import com.example.quiropracticoapi.repository.FacturaRepository;
import com.example.quiropracticoapi.service.FacturaAsyncService;
import com.example.quiropracticoapi.service.StorageService;
import com.example.quiropracticoapi.util.StoragePathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturaAsyncServiceImpl implements FacturaAsyncService {

    private static final Logger log = LoggerFactory.getLogger(FacturaAsyncServiceImpl.class);

    private final FacturaRepository facturaRepository;
    private final PdfFacturaService pdfFacturaService;
    private final StorageService storageService;
    private final StoragePathBuilder storagePathBuilder;
    private final DocumentoClienteRepository documentoClienteRepository;
    private final ClinicaRepository clinicaRepository;

    @Autowired
    public FacturaAsyncServiceImpl(FacturaRepository facturaRepository,
                                   PdfFacturaService pdfFacturaService,
                                   StorageService storageService,
                                   StoragePathBuilder storagePathBuilder,
                                   DocumentoClienteRepository documentoClienteRepository,
                                   ClinicaRepository clinicaRepository) {
        this.facturaRepository = facturaRepository;
        this.pdfFacturaService = pdfFacturaService;
        this.storageService = storageService;
        this.storagePathBuilder = storagePathBuilder;
        this.documentoClienteRepository = documentoClienteRepository;
        this.clinicaRepository = clinicaRepository;
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarPdfYDocumentoAsync(Integer idFactura) {
        if (idFactura == null) {
            log.warn("idFactura nulo recibido en procesarPdfYDocumentoAsync");
            return;
        }

        Factura factura = facturaRepository.findById(idFactura).orElse(null);
        if (factura == null) {
            log.error("Factura #{} no encontrada para procesar PDF y documento de forma asíncrona.", idFactura);
            return;
        }

        // Prevención de LazyInitializationException para el procesamiento en hilo @Async
        factura.getClinica().getNombreClinica();
        if (factura.getCliente() != null) {
            factura.getCliente().getNombre();
        }
        factura.getLineas().size();

        Long clinicaId = factura.getClinica().getIdClinica();
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId(clinicaId);
        }

        try {
            // 1. Generar PDF de la factura
            byte[] pdfBytes = pdfFacturaService.generarPdfFactura(factura);
            Integer idCliente = factura.getCliente() != null ? factura.getCliente().getIdCliente() : 0;

            // 2. Construir ruta R2 y subir el binario a Cloudflare R2
            String rutaR2 = storagePathBuilder.buildFacturacionPath(
                    clinicaId,
                    idCliente,
                    factura.getId(),
                    ".pdf",
                    factura.getFechaEmision()
            );

            storageService.storeBytes(pdfBytes, rutaR2, "application/pdf");

            // 3. Actualizar ruta R2 en la Factura
            factura.setRutaPdfR2(rutaR2);
            facturaRepository.save(factura);

            // 4. Integrar con el Gestor Documental (documentos_cliente)
            if (factura.getCliente() != null) {
                DocumentoCliente doc = new DocumentoCliente();
                doc.setCliente(factura.getCliente());
                doc.setClinica(factura.getClinica());
                doc.setNombreOriginal("Factura_" + factura.getNumeroFactura() + ".pdf");
                doc.setPathArchivo(rutaR2);
                doc.setTipoDocumento(TipoDocumento.FACTURA);
                doc.setEtiqueta("Factura " + factura.getNumeroFactura());
                doc.setMimeType("application/pdf");
                doc.setEstadoSubida(EstadoSubida.ACTIVO);
                doc.setTamanyoBytes((long) pdfBytes.length);
                doc.setActivo(true);
                if (factura.getPagosAsociados() != null && !factura.getPagosAsociados().isEmpty()) {
                    doc.setPago(factura.getPagosAsociados().get(0));
                }

                documentoClienteRepository.save(doc);

                // Actualizar cuota de almacenamiento consumida
                clinicaRepository.incrementarAlmacenamiento(clinicaId, (long) pdfBytes.length);
            }

            log.info("Factura {} procesada y archivada asíncronamente con éxito. Ruta R2: {}", 
                    factura.getNumeroFactura(), rutaR2);

        } catch (Exception e) {
            log.error("Fallo durante el procesamiento asíncrono de la factura #{}: {}", idFactura, e.getMessage(), e);
        }
    }
}
