-- ==============================================================================
-- V5: Motor de Facturación legal español y vinculación con Pagos
-- ==============================================================================

-- 1. Tabla de secuencias correlativas anuales por clínica (Optimistic Locking)
CREATE TABLE secuencias_facturacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    clinica_id BIGINT NOT NULL,
    prefijo_anio VARCHAR(10) NOT NULL,
    ultimo_numero INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_secuencias_clinica FOREIGN KEY (clinica_id) REFERENCES clinicas(id_clinica),
    UNIQUE KEY uk_secuencias_clinica_prefijo (clinica_id, prefijo_anio)
);

-- 2. Tabla principal de Facturas (Nominales y Simplificadas)
CREATE TABLE facturas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    clinica_id BIGINT NOT NULL,
    cliente_id INT NULL,
    numero_factura VARCHAR(50) NOT NULL,
    fecha_emision DATETIME(6) NOT NULL,
    base_imponible DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    tipo_factura VARCHAR(20) NOT NULL,
    ruta_pdf_r2 VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT uk_facturas_numero UNIQUE (numero_factura),
    CONSTRAINT fk_facturas_clinica FOREIGN KEY (clinica_id) REFERENCES clinicas(id_clinica),
    CONSTRAINT fk_facturas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id_cliente)
);

-- 3. Tabla de desglose de Líneas de Factura
CREATE TABLE lineas_factura (
    id INT AUTO_INCREMENT PRIMARY KEY,
    factura_id INT NOT NULL,
    concepto VARCHAR(255) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    impuestos DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_lineas_factura FOREIGN KEY (factura_id) REFERENCES facturas(id) ON DELETE CASCADE
);

-- 4. Modificación de la tabla pagos para asociar facturación
ALTER TABLE pagos ADD COLUMN id_factura INT NULL;
ALTER TABLE pagos ADD CONSTRAINT fk_pagos_factura FOREIGN KEY (id_factura) REFERENCES facturas(id);
