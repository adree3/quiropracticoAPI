-- ==============================================================================
-- V6: Añadir campos de auditoría BaseAuditEntity y ajustar constraint multi-tenant en facturas
-- ==============================================================================

ALTER TABLE facturas ADD COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE facturas ADD COLUMN ultima_modificacion DATETIME(6) NULL;
ALTER TABLE facturas ADD COLUMN creado_por VARCHAR(100) NULL;
ALTER TABLE facturas ADD COLUMN modificado_por VARCHAR(100) NULL;
ALTER TABLE facturas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE facturas DROP INDEX uk_facturas_numero;
ALTER TABLE facturas ADD CONSTRAINT uk_facturas_clinica_numero UNIQUE (clinica_id, numero_factura);
