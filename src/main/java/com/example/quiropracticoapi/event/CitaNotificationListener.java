package com.example.quiropracticoapi.event;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.service.impl.WhatsAppCloudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Collections;

@Slf4j
@Component
public class CitaNotificationListener {

    private final CitaRepository citaRepository;
    private final WhatsAppCloudService whatsAppCloudService;

    public CitaNotificationListener(CitaRepository citaRepository, WhatsAppCloudService whatsAppCloudService) {
        this.citaRepository = citaRepository;
        this.whatsAppCloudService = whatsAppCloudService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCitaCreada(CitaCreadaEvent event) {
        try {
            log.info("Procesando evento asíncrono CitaCreadaEvent para Cita ID: {}", event.idCita());
            
            // Inyectamos SecurityContext para que AuditorAwareImpl asigne "WHATSAPP_BOT"
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("WHATSAPP_BOT", null, Collections.emptyList())
            );

            // Cargamos la cita
            Cita cita = citaRepository.findById(event.idCita()).orElse(null);
            if (cita == null) {
                log.warn("Cita ID {} no encontrada al intentar enviar notificación.", event.idCita());
                return;
            }

            // Enviamos notificación
            String wamid = whatsAppCloudService.enviarConfirmacionCita(cita);
            
            // Actualizamos si hay WAMID (sea simulado o real)
            if (wamid != null && !wamid.isEmpty()) {
                cita.setWamidUltimoMensaje(wamid);
                citaRepository.save(cita); // Provocará auditoría con "WHATSAPP_BOT"
            }

        } catch (Exception e) {
            log.error("Fallo inesperado al enviar confirmación de cita {}: {}", event.idCita(), e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
