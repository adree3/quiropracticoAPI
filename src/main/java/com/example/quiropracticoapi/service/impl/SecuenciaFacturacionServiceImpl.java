package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.model.Clinica;
import com.example.quiropracticoapi.model.SecuenciaFacturacion;
import com.example.quiropracticoapi.repository.SecuenciaFacturacionRepository;
import com.example.quiropracticoapi.service.SecuenciaFacturacionService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Year;

@Service
public class SecuenciaFacturacionServiceImpl implements SecuenciaFacturacionService {

    private static final Logger log = LoggerFactory.getLogger(SecuenciaFacturacionServiceImpl.class);
    private static final int MAX_INTENTOS = 3;

    private final SecuenciaFacturacionRepository secuenciaFacturacionRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @Autowired
    public SecuenciaFacturacionServiceImpl(SecuenciaFacturacionRepository secuenciaFacturacionRepository,
                                           PlatformTransactionManager transactionManager) {
        this.secuenciaFacturacionRepository = secuenciaFacturacionRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public String generarSiguienteNumero(Clinica clinica) {
        if (clinica == null || clinica.getIdClinica() == null) {
            throw new IllegalArgumentException("La clínica es obligatoria para generar el número de factura.");
        }

        String prefijoAnio = String.valueOf(Year.now().getValue());

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                return requiresNewTransactionTemplate.execute(status -> {
                    SecuenciaFacturacion secuencia = secuenciaFacturacionRepository
                            .findByClinicaIdClinicaAndPrefijoAnio(clinica.getIdClinica(), prefijoAnio)
                            .orElseGet(() -> {
                                SecuenciaFacturacion nueva = new SecuenciaFacturacion();
                                nueva.setClinica(clinica);
                                nueva.setPrefijoAnio(prefijoAnio);
                                nueva.setUltimoNumero(0);
                                nueva.setVersion(0L);
                                return nueva;
                            });

                    int nuevoNumero = secuencia.getUltimoNumero() + 1;
                    secuencia.setUltimoNumero(nuevoNumero);

                    secuenciaFacturacionRepository.saveAndFlush(secuencia);

                    return String.format("F-%s-%05d", prefijoAnio, nuevoNumero);
                });
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException | DataIntegrityViolationException e) {
                log.warn("Colisión de concurrencia al generar secuencia de factura para clínica {} y año {} (intento {}/{}): {}",
                        clinica.getIdClinica(), prefijoAnio, intento, MAX_INTENTOS, e.getMessage());

                if (intento >= MAX_INTENTOS) {
                    throw new IllegalStateException("No se pudo generar un número de factura correlativo tras "
                            + MAX_INTENTOS + " intentos por alta concurrencia.", e);
                }

                try {
                    Thread.sleep(50L * intento);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupción durante el reintento de secuencia", ie);
                }
            }
        }

        throw new IllegalStateException("Error inesperado al generar número de factura.");
    }
}
