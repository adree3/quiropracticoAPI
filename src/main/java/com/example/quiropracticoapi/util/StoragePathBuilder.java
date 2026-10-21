package com.example.quiropracticoapi.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StoragePathBuilder {

    public static final String BASE_TENANT_PATH = "clinicas/%d/";

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateClinicaId(Long clinicaId) {
        if (clinicaId == null || clinicaId <= 0) {
            throw new IllegalArgumentException("El clinicaId es obligatorio para garantizar el aislamiento multi-tenant en Cloudflare R2.");
        }
    }

    /**
     * Justificante de Cita / Sesión individual firmada:
     * clinicas/{clinicaId}/clientes/{idCliente}/justificantes/Justificante_Sesion_{idCita}.pdf
     */
    public String buildRutaJustificante(Long clinicaId, Integer idCliente, Integer idCita) {
        validateClinicaId(clinicaId);
        return String.format(BASE_TENANT_PATH + "clientes/%d/justificantes/Justificante_Sesion_%d.pdf", clinicaId, idCliente, idCita);
    }

    /**
     * Justificante de Bono acumulado firmado:
     * clinicas/{clinicaId}/clientes/{idCliente}/justificantes/{servicioClean}_Bono_{idBonoActivo}.pdf
     */
    public String buildRutaJustificanteBono(Long clinicaId, Integer idCliente, String servicioClean, Integer idBonoActivo) {
        validateClinicaId(clinicaId);
        String clean = sanitize(servicioClean);
        return String.format(BASE_TENANT_PATH + "clientes/%d/justificantes/%s_Bono_%d.pdf", clinicaId, idCliente, clean, idBonoActivo);
    }

    /**
     * Documento médico categorizado dinámicamente:
     * clinicas/{clinicaId}/clientes/{idCliente}/{categoria}/{nombreArchivo}
     */
    public String buildRutaDocumentoMedico(Long clinicaId, Integer idCliente, String categoria, String nombreArchivo) {
        validateClinicaId(clinicaId);
        String catClean = sanitize(categoria);
        if (catClean.isBlank()) catClean = "general";
        String archClean = sanitize(nombreArchivo);
        return String.format(BASE_TENANT_PATH + "clientes/%d/%s/%s", clinicaId, idCliente, catClean, archClean);
    }

    /**
     * Facturación / Justificante de cobro:
     * clinicas/{clinicaId}/facturacion/{año}/{mes}/{idCliente}_{idDocumento}.ext
     */
    public String buildFacturacionPath(Long clinicaId, Integer idCliente, Integer idDocumento, String extension, LocalDateTime fecha) {
        validateClinicaId(clinicaId);
        String year = String.valueOf(fecha.getYear());
        String month = String.format("%02d", fecha.getMonthValue());
        String extClean = sanitize(extension);
        if (!extClean.isEmpty() && !extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "facturacion/%s/%s/%d_%d%s", clinicaId, year, month, idCliente, idDocumento, extClean);
    }

    /**
     * Pruebas Médicas:
     * clinicas/{clinicaId}/clientes/{idCliente}/pruebas_medicas/{baseName}.ext
     */
    public String buildPruebasMedicasPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        validateClinicaId(clinicaId);
        String cleanBaseName = sanitize(baseName);
        String extClean = sanitize(extension);
        if (!extClean.isEmpty() && !extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "clientes/%d/pruebas_medicas/%s%s", clinicaId, idCliente, cleanBaseName, extClean);
    }

    /**
     * Consentimientos (LOPD / Tratamiento):
     * clinicas/{clinicaId}/clientes/{idCliente}/consentimientos/{baseName}.ext
     */
    public String buildConsentimientosPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        validateClinicaId(clinicaId);
        String cleanBaseName = sanitize(baseName);
        String extClean = sanitize(extension);
        if (extClean.isEmpty()) {
            extClean = ".pdf";
        } else if (!extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "clientes/%d/consentimientos/%s%s", clinicaId, idCliente, cleanBaseName, extClean);
    }

    /**
     * Informes Médicos:
     * clinicas/{clinicaId}/clientes/{idCliente}/informes/{baseName}.ext
     */
    public String buildInformesPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        validateClinicaId(clinicaId);
        String cleanBaseName = sanitize(baseName);
        String extClean = sanitize(extension);
        if (!extClean.isEmpty() && !extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "clientes/%d/informes/%s%s", clinicaId, idCliente, cleanBaseName, extClean);
    }

    /**
     * Documentos Genéricos:
     * clinicas/{clinicaId}/clientes/{idCliente}/documentos/{baseName}.ext
     */
    public String buildDocumentosGenericosPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        validateClinicaId(clinicaId);
        String cleanBaseName = sanitize(baseName);
        String extClean = sanitize(extension);
        if (!extClean.isEmpty() && !extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "clientes/%d/documentos/%s%s", clinicaId, idCliente, cleanBaseName, extClean);
    }

    /**
     * Fotos de Perfil:
     * clinicas/{clinicaId}/usuarios/{idUsuario}/perfil/foto_perfil.ext
     */
    public String buildFotoPerfilPath(Long clinicaId, Integer idUsuario, String extension) {
        validateClinicaId(clinicaId);
        String extClean = sanitize(extension);
        if (!extClean.isEmpty() && !extClean.startsWith(".")) {
            extClean = "." + extClean;
        }
        return String.format(BASE_TENANT_PATH + "usuarios/%d/perfil/foto_perfil%s", clinicaId, idUsuario, extClean);
    }

    /**
     * Miniaturas optimizadas:
     * {originalPath}_thumb.jpg
     */
    public String buildThumbnailPath(String originalPath) {
        if (originalPath == null || originalPath.isBlank()) {
            throw new IllegalArgumentException("El path original es obligatorio para generar la miniatura.");
        }
        int dotIndex = originalPath.lastIndexOf('.');
        if (dotIndex == -1) return originalPath + "_thumb.jpg";
        return originalPath.substring(0, dotIndex) + "_thumb.jpg";
    }

    /**
     * Uploads con control tenant:
     * clinicas/{clinicaId}/uploads/{timestamp}_{originalFilename}
     */
    public String buildLegacyUploadPath(Long clinicaId, String timestamp, String originalFilename) {
        validateClinicaId(clinicaId);
        String cleanTimestamp = sanitize(timestamp);
        String cleanFilename = sanitize(originalFilename);
        return String.format(BASE_TENANT_PATH + "uploads/%s_%s", clinicaId, cleanTimestamp, cleanFilename);
    }
}
