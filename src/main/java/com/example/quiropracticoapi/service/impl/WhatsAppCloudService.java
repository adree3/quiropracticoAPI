package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.model.Cita;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Clinica;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppCloudService {

    @Value("${whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${whatsapp.api-version:v21.0}")
    private String apiVersion;

    @Value("${whatsapp.phone-number-id:dummy_phone_id}")
    private String globalPhoneNumberId;

    @Value("${whatsapp.access-token:dummy_token}")
    private String globalAccessToken;

    @Value("${whatsapp.template.confirmacion:confirmacion_cita_clinica}")
    private String templateConfirmacion;

    @Value("${whatsapp.template.recordatorio:recordatorio_cita_24h}")
    private String templateRecordatorio;

    private final RestClient restClient;

    public WhatsAppCloudService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
    }

    private String formatearTelefono(Cliente cliente) {
        String full = (cliente.getPrefijoTelefono() != null ? cliente.getPrefijoTelefono() : "") + 
                      (cliente.getTelefono() != null ? cliente.getTelefono() : "");
        return full.replace("+", "").replace(" ", "").trim();
    }

    private String getPhoneNumberId(Clinica clinica) {
        if (clinica != null && clinica.getWhatsappPhoneNumberId() != null && !clinica.getWhatsappPhoneNumberId().isBlank()) {
            return clinica.getWhatsappPhoneNumberId();
        }
        return globalPhoneNumberId;
    }

    private String getAccessToken(Clinica clinica) {
        if (clinica != null && clinica.getWhatsappAccessToken() != null && !clinica.getWhatsappAccessToken().isBlank()) {
            return clinica.getWhatsappAccessToken();
        }
        return globalAccessToken;
    }

    public String enviarConfirmacionCita(Cita cita) {
        return enviarPlantilla(cita, templateConfirmacion);
    }

    public String enviarRecordatorio24h(Cita cita) {
        return enviarPlantilla(cita, templateRecordatorio);
    }

    @SuppressWarnings("unchecked")
    private String enviarPlantilla(Cita cita, String templateName) {
        Cliente cliente = cita.getCliente();
        Clinica clinica = cita.getClinica();
        String to = formatearTelefono(cliente);
        String phoneNumberId = getPhoneNumberId(clinica);
        String accessToken = getAccessToken(clinica);

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", to,
            "type", "template",
            "template", Map.of(
                "name", templateName,
                "language", Map.of("code", "es_ES"),
                "components", List.of(
                    Map.of(
                        "type", "body",
                        "parameters", List.of(
                            Map.of("type", "text", "text", cliente.getNombre()),
                            Map.of("type", "text", "text", cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                            Map.of("type", "text", "text", cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"))),
                            Map.of("type", "text", "text", clinica != null ? clinica.getNombre() : "Clínica")
                        )
                    )
                )
            )
        );

        if (!whatsappEnabled) {
            log.info("[SIMULADO] WhatsApp API no está habilitada. Payload a Meta ({}): {}", templateName, body);
            return "wamid.SIMULATED_" + System.currentTimeMillis();
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            
            if (response != null && response.containsKey("messages")) {
                List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
                if (!messages.isEmpty()) {
                    return (String) messages.get(0).get("id");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error al enviar WhatsApp (template: {}): {}", templateName, e.getMessage());
            return null;
        }
    }

    public void enviarMensajeTextoLibre(Clinica clinica, String telefonoDestino, String texto) {
        String phoneNumberId = getPhoneNumberId(clinica);
        String accessToken = getAccessToken(clinica);

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", telefonoDestino,
            "type", "text",
            "text", Map.of("body", texto)
        );

        if (!whatsappEnabled) {
            log.info("[SIMULADO] WhatsApp Texto Libre a {}: {}", telefonoDestino, body);
            return;
        }

        try {
            restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error al enviar mensaje libre por WhatsApp a {}: {}", telefonoDestino, e.getMessage());
        }
    }
}
