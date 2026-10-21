package com.example.quiropracticoapi.scheduler;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.service.impl.WhatsAppCloudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class WhatsAppReminderScheduler {

    private final CitaRepository citaRepository;
    private final WhatsAppCloudService whatsAppCloudService;

    public WhatsAppReminderScheduler(CitaRepository citaRepository, WhatsAppCloudService whatsAppCloudService) {
        this.citaRepository = citaRepository;
        this.whatsAppCloudService = whatsAppCloudService;
    }

    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    @Transactional
    public void enviarRecordatorios24h() {
        log.info("Iniciando cron de recordatorios de WhatsApp 24h...");
        try {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("CRON_RECORDATORIOS", null, Collections.emptyList())
            );

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime inicio = now.plusHours(23);
            LocalDateTime fin = now.plusHours(25);

            List<Cita> citas = citaRepository.findCitasParaRecordatorio(inicio, fin);
            log.info("Se encontraron {} citas para enviar recordatorio.", citas.size());

            for (Cita cita : citas) {
                try {
                    String wamid = whatsAppCloudService.enviarRecordatorio24h(cita);
                    cita.setRecordatorioEnviado(true);
                    if (wamid != null && !wamid.isEmpty()) {
                        cita.setWamidUltimoMensaje(wamid);
                    }
                    citaRepository.save(cita);
                } catch (Exception e) {
                    log.error("Error al enviar recordatorio a la cita ID {}: {}", cita.getIdCita(), e.getMessage());
                }
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
        log.info("Cron de recordatorios finalizado.");
    }
}
