package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Clinica;
import com.example.quiropracticoapi.model.Factura;
import com.example.quiropracticoapi.model.LineaFactura;
import com.example.quiropracticoapi.model.Pago;
import com.example.quiropracticoapi.model.enums.EstadoFactura;
import com.example.quiropracticoapi.model.enums.TipoFactura;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.FacturaRepository;
import com.example.quiropracticoapi.repository.PagoRepository;
import com.example.quiropracticoapi.service.AuditoriaService;
import com.example.quiropracticoapi.service.FacturaAsyncService;
import com.example.quiropracticoapi.service.FacturacionService;
import com.example.quiropracticoapi.service.SecuenciaFacturacionService;
import com.example.quiropracticoapi.service.StorageService;
import com.example.quiropracticoapi.util.StoragePathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FacturacionServiceImpl implements FacturacionService {

    private static final Logger log = LoggerFactory.getLogger(FacturacionServiceImpl.class);

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final SecuenciaFacturacionService secuenciaFacturacionService;
    private final StorageService storageService;
    private final AuditoriaService auditoriaService;
    private final FacturaAsyncService facturaAsyncService;

    @Autowired
    public FacturacionServiceImpl(FacturaRepository facturaRepository,
                                  PagoRepository pagoRepository,
                                  SecuenciaFacturacionService secuenciaFacturacionService,
                                  StorageService storageService,
                                  AuditoriaService auditoriaService,
                                  FacturaAsyncService facturaAsyncService) {
        this.facturaRepository = facturaRepository;
        this.pagoRepository = pagoRepository;
        this.secuenciaFacturacionService = secuenciaFacturacionService;
        this.storageService = storageService;
        this.auditoriaService = auditoriaService;
        this.facturaAsyncService = facturaAsyncService;
    }

    @Override
    @Transactional
    public Factura emitirFacturaPorPagos(List<Integer> idsPagos, Cliente cliente, Clinica clinica, TipoFactura tipoFactura) {
        // 1. Validaciones de entrada
        if (idsPagos == null || idsPagos.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un pago para emitir la factura.");
        }
        if (clinica == null || clinica.getIdClinica() == null) {
            throw new IllegalArgumentException("La clínica es obligatoria para emitir una factura.");
        }

        List<Pago> pagos = pagoRepository.findAllById(idsPagos);
        if (pagos.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron los pagos especificados.");
        }
        if (pagos.size() != idsPagos.size()) {
            throw new ResourceNotFoundException("Uno o más pagos solicitados no existen en el sistema.");
        }

        // 2. Validaciones de negocio: pertenencia a clínica, doble facturación y cliente
        for (Pago pago : pagos) {
            if (pago.getClinica() == null || !pago.getClinica().getIdClinica().equals(clinica.getIdClinica())) {
                throw new IllegalArgumentException("El pago #" + pago.getIdPago() + " no pertenece a la clínica emisora.");
            }
            if (pago.getFactura() != null) {
                throw new IllegalStateException("El pago #" + pago.getIdPago() + " ya está vinculado a la factura " 
                        + pago.getFactura().getNumeroFactura() + " (prevención de doble facturación).");
            }
            if (cliente != null && pago.getCliente() != null && !pago.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
                throw new IllegalArgumentException("El pago #" + pago.getIdPago() + " pertenece a otro paciente.");
            }
        }

        // Si no se proporcionó cliente explícito, se toma el cliente del primer pago si existe
        Cliente clienteAsignado = cliente != null ? cliente : pagos.get(0).getCliente();
        TipoFactura tipoFinal = tipoFactura != null ? tipoFactura 
                : (clienteAsignado != null ? TipoFactura.COMPLETA : TipoFactura.SIMPLIFICADA);

        // 3. Cálculos de Base Imponible y Total (exento de IVA según art. 20.Uno.3 Ley 37/1992)
        BigDecimal baseImponible = BigDecimal.ZERO;
        for (Pago pago : pagos) {
            if (pago.getMonto() != null) {
                baseImponible = baseImponible.add(pago.getMonto());
            }
        }
        baseImponible = baseImponible.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = baseImponible;

        // 4. Instanciación y persistencia inicial de la Factura (Managed)
        Factura factura = new Factura();
        factura.setClinica(clinica);
        factura.setCliente(clienteAsignado);
        factura.setNumeroFactura(secuenciaFacturacionService.generarSiguienteNumero(clinica));
        factura.setFechaEmision(LocalDateTime.now());
        factura.setBaseImponible(baseImponible);
        factura.setTotal(total);
        factura.setTipoFactura(tipoFinal);
        factura.setEstado(EstadoFactura.EMITIDA);

        factura = facturaRepository.save(factura);

        // 5. Generación de Líneas de Factura y asociación bidireccional con Pagos
        for (Pago pago : pagos) {
            LineaFactura linea = new LineaFactura();
            
            String concepto = (pago.getServicioPagado() != null && pago.getServicioPagado().getNombreServicio() != null)
                    ? pago.getServicioPagado().getNombreServicio()
                    : (pago.getNotas() != null && !pago.getNotas().isBlank() ? pago.getNotas() : "Servicio Quiropráctico");

            BigDecimal monto = pago.getMonto() != null ? pago.getMonto().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            linea.setConcepto(concepto);
            linea.setCantidad(1);
            linea.setPrecioUnitario(monto);
            linea.setSubtotal(monto);
            linea.setImpuestos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            // Relación bidireccional Factura <-> LineaFactura
            linea.setFactura(factura);
            factura.getLineas().add(linea);

            // Relación bidireccional Factura <-> Pago
            pago.setFactura(factura);
            factura.getPagosAsociados().add(pago);
        }

        // 6. Persistencia de Líneas en cascada y actualización de Pagos con FK
        Factura facturaGuardada = facturaRepository.save(factura);
        pagoRepository.saveAll(pagos);

        // 7. Generación de PDF, subida a R2 y registro documental asíncrono (solo tras COMMIT exitoso)
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    facturaAsyncService.procesarPdfYDocumentoAsync(facturaGuardada.getId());
                }
            }
        );

        return facturaGuardada;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerUrlFacturaPorPago(Integer idPago) {
        if (idPago == null) {
            throw new IllegalArgumentException("El id del pago es obligatorio.");
        }

        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago #" + idPago + " no encontrado."));

        if (pago.getFactura() == null) {
            throw new IllegalStateException("El pago #" + idPago + " no tiene ninguna factura vinculada.");
        }

        Factura factura = pago.getFactura();
        if (factura.getRutaPdfR2() == null || factura.getRutaPdfR2().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El archivo PDF de la factura " 
                    + factura.getNumeroFactura() + " se está procesando en segundo plano. Por favor, reintente en unos segundos.");
        }

        String url = storageService.generatePresignedUrl(factura.getRutaPdfR2());

        String username = (SecurityContextHolder.getContext().getAuthentication() != null)
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "SISTEMA";

        try {
            auditoriaService.registrarAccion(
                    TipoAccion.DESCARGAR,
                    "FACTURA",
                    factura.getId().toString(),
                    "Descarga de factura " + factura.getNumeroFactura() + " vinculada al pago " + idPago,
                    username
            );
        } catch (Exception e) {
            log.error("Error al registrar auditoría de descarga de factura {} para pago {}: {}",
                    factura.getId(), idPago, e.getMessage());
        }

        return url;
    }
}
