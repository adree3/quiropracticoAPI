package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber; // El número del Sandbox (+1415...)

    @Autowired
    private AuditoriaServiceImpl auditoriaServiceImpl;

    // Inicializamos Twilio al arrancar la aplicación
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void enviarMensajeCita(String telefonoDestino, String nombrePaciente, String fechaHora, String sesionesRestantes, String nombreServicio) {
        try {
            String numeroFormateado = "whatsapp:" + (telefonoDestino.startsWith("+") ? telefonoDestino : "+34" + telefonoDestino);
            String numeroOrigen = "whatsapp:" + fromNumber;

            int sesiones = Integer.parseInt(sesionesRestantes);

            // Lógica del mensaje de saldo
            String mensajeSaldo;
            if (sesiones > 0) {
                mensajeSaldo = String.format("Te quedan *%s sesiones* disponibles.", sesionesRestantes);
            } else {
                // Mensaje sutil para cuando se acaba
                mensajeSaldo = "⚠ *Nota:* Este bono se ha agotado con esta cita. Recuerda renovarlo en tu próxima visita.";
            }

            String cuerpoMensaje = String.format(
                    "👋 Hola *%s*,\n\n" +
                            "✅ *CITA CONFIRMADA*\n" +
                            "📅 *Fecha:* %s\n" +
                            "💆‍♂️ *Tratamiento:* %s\n\n" +
                            "💳 *Estado del Bono:*\n" +
                            "Se ha descontado 1 sesión. %s\n\n",
                    nombrePaciente, fechaHora, nombreServicio, mensajeSaldo
            );

            Message message = Message.creator(
                    new PhoneNumber(numeroFormateado),
                    new PhoneNumber(numeroOrigen),
                    cuerpoMensaje
            ).create();
            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.NOTIFICACION,
                    "WHATSAPP",
                    nombrePaciente,
                    "Mensaje enviado a " + telefonoDestino + ". SID: " + message.getSid()
            );
            System.out.println("Mensaje enviado con SID: " + message.getSid());

        } catch (Exception e) {
            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.ERROR,
                    "WHATSAPP",
                    nombrePaciente,
                    "FALLO de envío: " + e.getMessage()
            );
            // Importante: Que un fallo en WhatsApp no tumbe tu servidor
            System.err.println("Error enviando WhatsApp: " + e.getMessage());
        }
    }
}
