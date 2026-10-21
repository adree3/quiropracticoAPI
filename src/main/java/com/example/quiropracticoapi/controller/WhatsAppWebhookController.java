package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.service.impl.WhatsAppCloudService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@Tag(name = "WhatsApp Webhooks", description = "Recibe notificaciones de Meta WhatsApp Cloud API")
public class WhatsAppWebhookController {

    @Value("${whatsapp.verify-token:quiro_webhook_secret_2026}")
    private String verifyToken;

    private final CitaRepository citaRepository;
    private final WhatsAppCloudService whatsAppCloudService;

    public WhatsAppWebhookController(CitaRepository citaRepository, WhatsAppCloudService whatsAppCloudService) {
        this.citaRepository = citaRepository;
        this.whatsAppCloudService = whatsAppCloudService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verificado con éxito.");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Fallo al verificar el webhook.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            if (entry == null || entry.isEmpty()) return ResponseEntity.ok().build();

            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
            if (changes == null || changes.isEmpty()) return ResponseEntity.ok().build();

            Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
            if (value == null) return ResponseEntity.ok().build();

            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) return ResponseEntity.ok().build();

            Map<String, Object> message = messages.get(0);
            String from = (String) message.get("from");
            String type = (String) message.get("type");

            String contextId = null;
            if (message.containsKey("context")) {
                Map<String, Object> context = (Map<String, Object>) message.get("context");
                contextId = (String) context.get("id"); // WAMID original
            }

            String botonId = null;
            String texto = null;

            if ("button".equals(type) && message.containsKey("button")) {
                Map<String, Object> button = (Map<String, Object>) message.get("button");
                botonId = (String) button.get("payload");
                texto = (String) button.get("text");
            } else if ("text".equals(type) && message.containsKey("text")) {
                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                texto = (String) textObj.get("body");
                botonId = texto;
            }

            if (from == null || botonId == null) {
                return ResponseEntity.ok().build();
            }

            log.info("Mensaje recibido de {} (Context: {}). Payload: {}", from, contextId, botonId);

            // Inyectamos el context para auditoría
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("WHATSAPP_PACIENTE:" + from, null, Collections.emptyList())
            );

            Cita citaDestino = null;
            if (contextId != null) {
                citaDestino = citaRepository.findByWamidUltimoMensaje(contextId).orElse(null);
            }
            if (citaDestino == null) {
                citaDestino = citaRepository.findProximaCitaPorTelefono(from, LocalDateTime.now().minusHours(2)).orElse(null);
            }

            if (citaDestino == null) {
                log.warn("No se encontró cita programada para el teléfono {} y WAMID {}", from, contextId);
                return ResponseEntity.ok().build(); // Contestamos 200 siempre a Meta
            }

            String respuesta = botonId.toUpperCase();
            if (respuesta.contains("CONFIRMAR") || respuesta.contains("CONFIRMAR_CITA") || respuesta.contains("SI") || respuesta.contains("SÍ")) {
                log.info("Cita ID {} confirmada por WhatsApp por el teléfono {}", citaDestino.getIdCita(), from);
                citaDestino.setConfirmadaWhatsapp(true);
                citaRepository.save(citaDestino);
                whatsAppCloudService.enviarMensajeTextoLibre(
                    citaDestino.getClinica(), from, "¡Gracias! Tu cita ha quedado confirmada. Te esperamos."
                );
            } else if (respuesta.contains("CANCELAR") || respuesta.contains("CANCELAR_CITA") || respuesta.contains("NO PUEDO")) {
                String telRecepcion = citaDestino.getClinica().getTelefonoAtencionHumana();
                if (telRecepcion == null || telRecepcion.isBlank()) {
                    telRecepcion = citaDestino.getClinica().getTelefono();
                }
                String mensajeAtencion = "Vaya, lamentamos que no puedas asistir. Por favor, contacta con recepción al " + 
                                         telRecepcion + " o a través del enlace https://wa.me/" + telRecepcion.replace("+", "").replace(" ", "") + 
                                         " para reagendar tu cita lo antes posible.";
                whatsAppCloudService.enviarMensajeTextoLibre(citaDestino.getClinica(), from, mensajeAtencion);
            } else {
                log.info("Mensaje no reconocido como acción de confirmación/cancelación: {}", respuesta);
            }

        } catch (Exception e) {
            log.error("Error al procesar webhook de WhatsApp: {}", e.getMessage(), e);
        } finally {
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok().build();
    }
}
