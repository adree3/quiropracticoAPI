ALTER TABLE documentos_cliente ADD COLUMN etiqueta VARCHAR(100);

UPDATE documentos_cliente SET tipo_documento = 'PRUEBA_MEDICA', etiqueta = tipo_documento WHERE tipo_documento IN ('RADIOGRAFIA', 'RESONANCIA');
UPDATE documentos_cliente SET tipo_documento = 'FACTURACION' WHERE tipo_documento = 'JUSTIFICANTE_PAGO';
