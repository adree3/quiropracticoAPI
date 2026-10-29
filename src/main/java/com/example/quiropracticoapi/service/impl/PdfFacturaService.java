package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.model.Factura;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfFacturaService {

    private final TemplateEngine templateEngine;

    @Autowired
    public PdfFacturaService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Genera el documento PDF oficial de la factura procesando factura.html mediante OpenPDF (ITextRenderer).
     */
    public byte[] generarPdfFactura(Factura factura) {
        try {
            Context context = new Context();

            String clinicaNombre = (factura.getClinica() != null && factura.getClinica().getNombre() != null)
                    ? factura.getClinica().getNombre() : "Clínica Quiropráctica";
            String clinicaCif = (factura.getClinica() != null && factura.getClinica().getCifNif() != null)
                    ? factura.getClinica().getCifNif() : "---";
            String clinicaDireccion = (factura.getClinica() != null && factura.getClinica().getDireccion() != null)
                    ? factura.getClinica().getDireccion() : "---";

            String clienteNombre = null;
            String clienteDni = null;
            if (factura.getCliente() != null) {
                String nom = factura.getCliente().getNombre() != null ? factura.getCliente().getNombre() : "";
                String ape = factura.getCliente().getApellidos() != null ? factura.getCliente().getApellidos() : "";
                clienteNombre = (nom + " " + ape).trim();
                clienteDni = factura.getCliente().getDniNie();
            }

            String fechaFormateada = factura.getFechaEmision() != null
                    ? factura.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "";

            context.setVariable("clinicaNombre", clinicaNombre);
            context.setVariable("clinicaCif", clinicaCif);
            context.setVariable("clinicaDireccion", clinicaDireccion);
            context.setVariable("numeroFactura", factura.getNumeroFactura());
            context.setVariable("fechaEmision", fechaFormateada);
            context.setVariable("clienteNombre", clienteNombre);
            context.setVariable("clienteDni", clienteDni);
            context.setVariable("tipoFactura", factura.getTipoFactura() != null ? factura.getTipoFactura().name() : "SIMPLIFICADA");
            context.setVariable("lineas", factura.getLineas());
            context.setVariable("baseImponible", String.format(Locale.GERMAN, "%.2f €", factura.getBaseImponible() != null ? factura.getBaseImponible() : 0.0));
            context.setVariable("total", String.format(Locale.GERMAN, "%.2f €", factura.getTotal() != null ? factura.getTotal() : 0.0));

            // Procesar plantilla HTML con Thymeleaf
            String htmlContent = templateEngine.process("factura", context);

            // Renderizar PDF binario con Flying Saucer / OpenPDF
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(bos);

            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la factura #" + factura.getNumeroFactura() + ": " + e.getMessage(), e);
        }
    }
}
