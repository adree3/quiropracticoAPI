-- Clínica base
INSERT INTO clinicas (nombre, subdominio, duracion_cita_minutos, limite_almacenamiento_bytes, almacenamiento_usado_bytes, activa, usar_dos_telefonos_whatsapp, fecha_creacion, version)
VALUES ('Quiropráctico Central', 'central', 30, 5368709120, 0, 1, 0, NOW(), 0);

-- Usuarios
-- Hash BCrypt de 'admin123' -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO usuarios (clinica_id, username, password_hash, nombre_completo, rol, activo, intentos_fallidos, cuenta_bloqueada, debe_cambiar_password, color_agenda, fecha_creacion, version)
VALUES 
(1, 'admin', '$2a$10$vGuRPqyXq6iAW5/cqaXz0OW/adfi4SjoAa.FwWC2aoKpHNsxvRbH.', 'Super Administrador', 'super_admin', 1, 0, 0, 0, '#3B82F6', NOW(), 0),
(1, 'doctor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Doctor Principal', 'doctor', 1, 0, 0, 0, '#2563EB', NOW(), 0);

UPDATE usuarios SET numero_colegiado = 'AEQ-1042' WHERE username = 'doctor1';

-- Servicios base
INSERT INTO servicios (clinica_id, nombre_servicio, precio, tipo, sesiones_incluidas, activo, fecha_creacion, version)
VALUES 
(1, 'Primera Visita', 60.00, 'sesion_unica', 1, 1, NOW(), 0),
(1, 'Bono 10 Sesiones', 350.00, 'bono', 10, 1, NOW(), 0);

-- Horarios base para doctor1 (ID = 2)
INSERT INTO horarios (id_usuario_quiro, clinica_id, dia_semana, hora_inicio, hora_fin)
VALUES 
(2, 1, 1, '09:00:00', '14:00:00'), (2, 1, 1, '16:00:00', '20:00:00'),
(2, 1, 2, '09:00:00', '14:00:00'), (2, 1, 2, '16:00:00', '20:00:00'),
(2, 1, 3, '09:00:00', '14:00:00'), (2, 1, 3, '16:00:00', '20:00:00'),
(2, 1, 4, '09:00:00', '14:00:00'), (2, 1, 4, '16:00:00', '20:00:00'),
(2, 1, 5, '09:00:00', '14:00:00'), (2, 1, 5, '16:00:00', '20:00:00');

-- Clientes de prueba
INSERT INTO clientes (clinica_id, nombre, apellidos, fecha_nacimiento, prefijo_telefono, telefono, email, sexo, activo, fecha_alta, version)
VALUES 
(1, 'María', 'López García', '1985-05-15', '+34', '600123456', 'maria.lopez@example.com', 'F', 1, NOW(), 0),
(1, 'Carlos', 'Martínez Ruiz', '1990-10-22', '+34', '611234567', 'carlos.martinez@example.com', 'M', 1, NOW(), 0),
(1, 'Lucía', 'Fernández Vega', '2000-01-30', '+34', '622345678', 'lucia.fernandez@example.com', 'F', 1, NOW(), 0);
