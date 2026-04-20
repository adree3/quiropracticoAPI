package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.model.Cita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PdfFirmaService {

    private final TemplateEngine templateEngine;

    @Autowired
    public PdfFirmaService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Clase auxiliar para los datos de cada sesión en la tabla del PDF
     */
    public static class SesionInfo {
        private int numeroSesion;
        private String fecha;
        private String hora;
        private String firmaBase64;

        public SesionInfo(int numeroSesion, String fecha, String hora, String firmaBase64) {
            this.numeroSesion = numeroSesion;
            this.fecha = fecha;
            this.hora = hora;
            this.firmaBase64 = firmaBase64;
        }

        public int getNumeroSesion() { return numeroSesion; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getFirmaBase64() { return firmaBase64; }
    }

    /**
     * Genera un PDF de la cartilla siguiendo la nueva plantilla.
     * Retorna los bytes del PDF generado para subir directamente a R2.
     */
    public byte[] generarPdfBono(
            String clienteNombre,
            String clienteId,
            String quiroNombre,
            String bonoNombre,
            String bonoId,
            String bonoCaducidad,
            java.util.List<SesionInfo> listaSesiones
    ) {
        try {
            Context context = new Context();
            context.setVariable("clienteNombre", clienteNombre);
            context.setVariable("clienteDni", clienteId); 
            context.setVariable("quiroNombre", quiroNombre);
            context.setVariable("bonoNombre", bonoNombre);
            context.setVariable("bonoId", bonoId);
            context.setVariable("bonoCaducidad", bonoCaducidad);
            context.setVariable("listaSesiones", listaSesiones);
            context.setVariable("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            // Procesamos la plantilla a HTML
            String htmlContent = templateEngine.process("justificante", context);

            // Generar PDF en memoria
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(bos);

            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la cartilla: " + e.getMessage(), e);
        }
    }

    /**
     * Método de limpieza de Base64 (Thymeleaf lo prefiere sin el prefijo data:image/...)
     */
    public static String limpiarBase64(String base64) {
        if (base64 == null) return null;
        String cleaned = base64.trim();
        if (cleaned.contains(",")) {
            return cleaned.split(",")[1];
        }
        return cleaned;
    }
}
