-- ==============================================================================
-- V7: Actualización de columnas para soportar nuevos enumerados (Auditoría y Documentos)
-- ==============================================================================

-- 1. Actualización de columna en documentos_cliente para soportar FACTURA y FACTURACION
ALTER TABLE documentos_cliente MODIFY COLUMN tipo_documento VARCHAR(50) NOT NULL;

-- 2. Actualización de columna en auditoria para soportar VISUALIZAR y DESCARGAR
ALTER TABLE auditoria MODIFY COLUMN accion VARCHAR(255) NOT NULL;
