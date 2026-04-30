package com.example.quiropracticoapi.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StoragePathBuilder {

    public static final String BASE_TENANT_PATH = "clinicas/%d/";
    
    // Facturación: clinicas/{clinicaId}/facturacion/{año}/{mes}/{idCliente}_{idDocumento}.ext
    public String buildFacturacionPath(Long clinicaId, Integer idCliente, Integer idDocumento, String extension, LocalDateTime fecha) {
        String year = String.valueOf(fecha.getYear());
        String month = String.format("%02d", fecha.getMonthValue());
        return String.format(BASE_TENANT_PATH + "facturacion/%s/%s/%d_%d%s", clinicaId, year, month, idCliente, idDocumento, extension);
    }
    
    // Pruebas Médicas: clinicas/{clinicaId}/clientes/{idCliente}/pruebas_medicas/{baseName}.ext
    public String buildPruebasMedicasPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        return String.format(BASE_TENANT_PATH + "clientes/%d/pruebas_medicas/%s%s", clinicaId, idCliente, baseName, extension);
    }
    
    // Consentimientos: clinicas/{clinicaId}/clientes/{idCliente}/consentimientos/{baseName}.ext
    public String buildConsentimientosPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        return String.format(BASE_TENANT_PATH + "clientes/%d/consentimientos/%s%s", clinicaId, idCliente, baseName, extension);
    }
    
    // Informes: clinicas/{clinicaId}/clientes/{idCliente}/informes/{baseName}.ext
    public String buildInformesPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        return String.format(BASE_TENANT_PATH + "clientes/%d/informes/%s%s", clinicaId, idCliente, baseName, extension);
    }
    
    // Documentos Genéricos/Citas: clinicas/{clinicaId}/clientes/{idCliente}/documentos/{baseName}.ext
    public String buildDocumentosGenericosPath(Long clinicaId, Integer idCliente, String baseName, String extension) {
        return String.format(BASE_TENANT_PATH + "clientes/%d/documentos/%s%s", clinicaId, idCliente, baseName, extension);
    }
    
    // Fotos de Perfil: clinicas/{clinicaId}/usuarios/{idUsuario}/perfil/foto_perfil.ext
    public String buildFotoPerfilPath(Long clinicaId, Integer idUsuario, String extension) {
        return String.format(BASE_TENANT_PATH + "usuarios/%d/perfil/foto_perfil%s", clinicaId, idUsuario, extension);
    }
    
    // Legacy Uploads: clinicas/{clinicaId}/uploads/{timestamp}_{originalFilename}
    public String buildLegacyUploadPath(Long clinicaId, String timestamp, String originalFilename) {
        return String.format(BASE_TENANT_PATH + "uploads/%s_%s", clinicaId, timestamp, originalFilename);
    }
}
