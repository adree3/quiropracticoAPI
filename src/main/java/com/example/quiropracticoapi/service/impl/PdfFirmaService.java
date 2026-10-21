package com.example.quiropracticoapi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * Genera el PDF del documento de consentimiento de Protección de Datos (LOPD/RGPD)
     * a partir de datos primitivos desacoplados de Hibernate para evitar LazyInitializationException.
     */
    public byte[] generarPdfLopd(String nombreClinica, String nombrePaciente, String dniPaciente, String firmaBase64) {
        try {
            Context context = new Context();

            String clinicClean = (nombreClinica != null && !nombreClinica.isBlank())
                    ? nombreClinica
                    : "Clínica Quiropráctica";

            String pacienteClean = (nombrePaciente != null && !nombrePaciente.isBlank())
                    ? nombrePaciente
                    : "Paciente";

            String dniClean = (dniPaciente != null && !dniPaciente.isBlank())
                    ? dniPaciente
                    : "No especificado";

            String fechaActual = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            context.setVariable("nombreClinica", clinicClean);
            context.setVariable("nombrePaciente", pacienteClean);
            context.setVariable("dniPaciente", dniClean);
            context.setVariable("fecha", fechaActual);
            context.setVariable("firmaBase64", limpiarBase64(firmaBase64));

            String htmlContent = templateEngine.process("consentimiento_lopd", context);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(bos);

            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de consentimiento LOPD: " + e.getMessage(), e);
        }
    }

    /**
     * Sobrecarga de conveniencia que extrae los datos de la entidad Cliente.
     */
    public byte[] generarPdfLopd(com.example.quiropracticoapi.model.Cliente cliente, String firmaBase64) {
        String nombreClinica = (cliente != null && cliente.getClinica() != null && cliente.getClinica().getNombre() != null)
                ? cliente.getClinica().getNombre()
                : "Clínica Quiropráctica";

        String nombrePaciente = cliente != null
                ? ((cliente.getNombre() != null ? cliente.getNombre() : "") + " " +
                   (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim()
                : "Paciente";

        String dniPaciente = (cliente != null && cliente.getDniNie() != null && !cliente.getDniNie().isBlank())
                ? cliente.getDniNie()
                : "No especificado";

        return generarPdfLopd(nombreClinica, nombrePaciente, dniPaciente, firmaBase64);
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
