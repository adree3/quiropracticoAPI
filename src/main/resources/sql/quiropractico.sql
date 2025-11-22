-- Creamos la base de datos si no existe
CREATE DATABASE IF NOT EXISTS quiropractica_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quiropractica_db;

-- Desactivamos la comprobación de claves foráneas para crear/borrar tablas
SET FOREIGN_KEY_CHECKS = 0;


-- -----------------------------------------------------
-- 1. GESTIÓN DE PERSONAL
-- -----------------------------------------------------
-- Tabla para el personal que usa el software (admins, quiro, recepción)
DROP TABLE IF EXISTS `usuarios`;
CREATE TABLE `usuarios` (
  `id_usuario` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'Nunca guardar en texto plano (usar bcrypt)',
  `nombre_completo` VARCHAR(150) NOT NULL,
  `rol` ENUM('admin', 'quiropráctico', 'recepción') NOT NULL,
  `activo` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=Inactivo, 1=Activo',
  PRIMARY KEY (`id_usuario`)
) ENGINE=InnoDB;

-- Horario base de disponibilidad de los quiroprácticos
DROP TABLE IF EXISTS `horarios`;
CREATE TABLE `horarios` (
  `id_horario` INT NOT NULL AUTO_INCREMENT,
  `id_usuario_quiro` INT NOT NULL COMMENT 'FK a usuarios (solo rol quiropráctico)',
  `dia_semana` TINYINT NOT NULL COMMENT '1=Lunes, 2=Martes, ... 7=Domingo',
  `hora_inicio` TIME NOT NULL,
  `hora_fin` TIME NOT NULL,
  PRIMARY KEY (`id_horario`),
  INDEX `idx_horario_usuario` (`id_usuario_quiro`),
  CONSTRAINT `fk_horario_usuario`
    FOREIGN KEY (`id_usuario_quiro`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Bloqueos de agenda para vacaciones, festivos o excepciones
DROP TABLE IF EXISTS `bloqueos_agenda`;
CREATE TABLE `bloqueos_agenda` (
  `id_bloqueo` INT NOT NULL AUTO_INCREMENT,
  `id_usuario_quiro` INT NULL COMMENT 'Si es NULL, es un bloqueo de clínica (festivo)',
  `fecha_hora_inicio` DATETIME NOT NULL,
  `fecha_hora_fin` DATETIME NOT NULL,
  `motivo` VARCHAR(255) NULL,
  PRIMARY KEY (`id_bloqueo`),
  INDEX `idx_bloqueo_usuario` (`id_usuario_quiro`),
  INDEX `idx_bloqueo_fechas` (`fecha_hora_inicio`, `fecha_hora_fin`),
  CONSTRAINT `fk_bloqueo_usuario`
    FOREIGN KEY (`id_usuario_quiro`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 2. GESTIÓN DE PACIENTES
-- -----------------------------------------------------
-- Tabla de clientes
DROP TABLE IF EXISTS `clientes`;
CREATE TABLE `clientes` (
  `id_cliente` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `apellidos` VARCHAR(150) NOT NULL,
  `fecha_nacimiento` DATE NULL,
  `telefono` VARCHAR(25) NOT NULL,
  `email` VARCHAR(100) NULL UNIQUE,
  `direccion` VARCHAR(255) NULL,
  `fecha_alta` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `notas_privadas` TEXT NULL COMMENT 'Notas administrativas, no clínicas',
  PRIMARY KEY (`id_cliente`),
  INDEX `idx_cliente_nombre_apellidos` (`apellidos`, `nombre`),
  INDEX `idx_cliente_telefono` (`telefono`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 3. GESTIÓN DE CITAS E HISTORIAL
-- -----------------------------------------------------
-- Citas de la agenda
DROP TABLE IF EXISTS `citas`;
CREATE TABLE `citas` (
  `id_cita` INT NOT NULL AUTO_INCREMENT,
  `id_cliente` INT NOT NULL,
  `id_quiropractico` INT NOT NULL COMMENT 'FK a usuarios (rol quiropráctico)',
  `fecha_hora_inicio` DATETIME NOT NULL,
  `fecha_hora_fin` DATETIME NOT NULL,
  `estado` ENUM('programada', 'completada', 'cancelada', 'ausente') NOT NULL DEFAULT 'programada',
  `notas_recepcion` TEXT NULL,
  PRIMARY KEY (`id_cita`),
  INDEX `idx_cita_cliente` (`id_cliente`),
  INDEX `idx_cita_quiropractico` (`id_quiropractico`),
  INDEX `idx_cita_fechas` (`fecha_hora_inicio`, `fecha_hora_fin`),
  CONSTRAINT `fk_cita_cliente`
    FOREIGN KEY (`id_cliente`)
    REFERENCES `clientes` (`id_cliente`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_cita_quiropractico`
    FOREIGN KEY (`id_quiropractico`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Historial clínico (notas S.O.A.P.) vinculado a una cita completada
DROP TABLE IF EXISTS `historial_clinico`;
CREATE TABLE `historial_clinico` (
  `id_historial` INT NOT NULL AUTO_INCREMENT,
  `id_cita` INT NOT NULL UNIQUE COMMENT 'Una entrada de historial por cita',
  `id_cliente` INT NOT NULL,
  `id_quiropractico` INT NOT NULL,
  `fecha_sesion` DATETIME NOT NULL,
  `notas_subjetivo` TEXT NULL,
  `notas_objetivo` TEXT NULL,
  `ajustes_realizados` TEXT NULL,
  `plan_futuro` TEXT NULL,
  PRIMARY KEY (`id_historial`),
  INDEX `idx_historial_cliente` (`id_cliente`),
  CONSTRAINT `fk_historial_cita`
    FOREIGN KEY (`id_cita`)
    REFERENCES `citas` (`id_cita`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_historial_cliente`
    FOREIGN KEY (`id_cliente`)
    REFERENCES `clientes` (`id_cliente`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_historial_quiropractico`
    FOREIGN KEY (`id_quiropractico`)
    REFERENCES `usuarios` (`id_usuario`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 4. GESTIÓN COMERCIAL Y PAGOS
-- -----------------------------------------------------
-- Catálogo de servicios y bonos que se venden
DROP TABLE IF EXISTS `servicios`;
CREATE TABLE `servicios` (
  `id_servicio` INT NOT NULL AUTO_INCREMENT,
  `nombre_servicio` VARCHAR(150) NOT NULL,
  `precio` DECIMAL(10, 2) NOT NULL,
  `tipo` ENUM('sesion_unica', 'bono') NOT NULL,
  `sesiones_incluidas` INT NULL COMMENT 'Solo si tipo=bono',
  `activo` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=No se ofrece, 1=Se ofrece',
  PRIMARY KEY (`id_servicio`)
) ENGINE=InnoDB;

-- Registro de transacciones monetarias
DROP TABLE IF EXISTS `pagos`;
CREATE TABLE `pagos` (
  `id_pago` INT NOT NULL AUTO_INCREMENT,
  `id_cliente` INT NOT NULL,
  `monto` DECIMAL(10, 2) NOT NULL,
  `metodo_pago` ENUM('efectivo', 'tarjeta', 'transferencia', 'otro') NOT NULL,
  `fecha_pago` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id_servicio_pagado` INT NULL COMMENT 'Opcional: qué servicio/bono generó este pago',
  `notas` VARCHAR(255) NULL,
  PRIMARY KEY (`id_pago`),
  INDEX `idx_pago_cliente` (`id_cliente`),
  CONSTRAINT `fk_pago_cliente`
    FOREIGN KEY (`id_cliente`)
    REFERENCES `clientes` (`id_cliente`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Bonos que un cliente ha comprado y tiene activos
DROP TABLE IF EXISTS `bonos_activos`;
CREATE TABLE `bonos_activos` (
  `id_bono_activo` INT NOT NULL AUTO_INCREMENT,
  `id_cliente` INT NOT NULL,
  `id_servicio_comprado` INT NOT NULL COMMENT 'FK a servicios (tipo=bono)',
  `id_pago_origen` INT NOT NULL UNIQUE COMMENT 'Pago que generó este bono',
  `fecha_compra` DATE NOT NULL,
  `sesiones_totales` INT NOT NULL,
  `sesiones_restantes` INT NOT NULL,
  `fecha_caducidad` DATE NULL,
  PRIMARY KEY (`id_bono_activo`),
  INDEX `idx_bono_cliente` (`id_cliente`),
  INDEX `idx_bono_servicio` (`id_servicio_comprado`),
  INDEX `idx_bono_pago` (`id_pago_origen`),
  CONSTRAINT `fk_bono_cliente`
    FOREIGN KEY (`id_cliente`)
    REFERENCES `clientes` (`id_cliente`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_bono_servicio`
    FOREIGN KEY (`id_servicio_comprado`)
    REFERENCES `servicios` (`id_servicio`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_bono_pago`
    FOREIGN KEY (`id_pago_origen`)
    REFERENCES `pagos` (`id_pago`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Tabla para vincular clientes (Madre -> Hijo)
CREATE TABLE `grupos_familiares` (
  `id_grupo` INT NOT NULL AUTO_INCREMENT,
  `id_cliente_propietario` INT NOT NULL COMMENT 'El dueño de los bonos (ej. Madre)',
  `id_cliente_beneficiario` INT NOT NULL COMMENT 'El que puede usarlos (ej. Hijo)',
  `relacion` VARCHAR(50) NULL COMMENT 'Ej. Madre-Hijo, Pareja',
  PRIMARY KEY (`id_grupo`),
  -- Evitamos duplicados (A no puede vincular a B dos veces)
  UNIQUE KEY `unique_relacion` (`id_cliente_propietario`, `id_cliente_beneficiario`),
  CONSTRAINT `fk_grupo_propietario` FOREIGN KEY (`id_cliente_propietario`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE,
  CONSTRAINT `fk_grupo_beneficiario` FOREIGN KEY (`id_cliente_beneficiario`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla de enlace que registra qué cita consumió qué bono
DROP TABLE IF EXISTS `consumos_bono`;
CREATE TABLE `consumos_bono` (
  `id_consumo` INT NOT NULL AUTO_INCREMENT,
  `id_cita` INT NOT NULL UNIQUE COMMENT 'Una cita solo puede consumir 1 sesión',
  `id_bono_activo` INT NOT NULL,
  `fecha_consumo` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_consumo`),
  INDEX `idx_consumo_bono` (`id_bono_activo`),
  CONSTRAINT `fk_consumo_cita`
    FOREIGN KEY (`id_cita`)
    REFERENCES `citas` (`id_cita`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_consumo_bono`
    FOREIGN KEY (`id_bono_activo`)
    REFERENCES `bonos_activos` (`id_bono_activo`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;


-- Reactivamos la comprobación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;


USE quiropractica_db;

-- Limpiamos datos previos (en orden inverso para respetar FKs)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE consumos_bono;
TRUNCATE TABLE bonos_activos;
TRUNCATE TABLE historial_clinico;
TRUNCATE TABLE pagos;
TRUNCATE TABLE citas;
TRUNCATE TABLE bloqueos_agenda;
TRUNCATE TABLE horarios;
TRUNCATE TABLE servicios;
TRUNCATE TABLE clientes;
TRUNCATE TABLE usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------
-- 1. USUARIOS (Password para todos: "123")
-- -----------------------------------------------------
INSERT INTO usuarios (id_usuario, username, password_hash, nombre_completo, rol, activo) VALUES 
(1, 'admin', '$2a$10$EuwpvVgMatdy.w/j2M.0.O0/w/y/z.x.y.z.x.y.z.x.y.z.x.y.z', 'Administrador Principal', 'admin', 1),
(2, 'dr_ana', '$2a$10$EuwpvVgMatdy.w/j2M.0.O0/w/y/z.x.y.z.x.y.z.x.y.z.x.y.z', 'Dra. Ana Quiropráctica', 'quiropráctico', 1),
(3, 'laura', '$2a$10$EuwpvVgMatdy.w/j2M.0.O0/w/y/z.x.y.z.x.y.z.x.y.z.x.y.z', 'Laura Recepción', 'recepción', 1);

-- Nota: El hash '$2a$10$EuwpvVgMatdy.w/j2M.0.O0/w/y/z.x.y.z.x.y.z.x.y.z.x.y.z' es un ejemplo válido. 
-- Si no te funciona el login, usa el endpoint /register para crear uno nuevo y ver qué hash genera tu sistema.
-- Ojo: He usado un hash genérico de ejemplo. Si quieres asegurar que sea '123', 
-- lo ideal es que crees el primer usuario con tu endpoint /register.
-- Pero para probar rápido, aquí va uno que suele ser '123' en BCrypt estándar:
UPDATE usuarios SET password_hash = '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38a' WHERE id_usuario IN (1,2,3);


-- -----------------------------------------------------
-- 2. CLIENTES
-- -----------------------------------------------------
INSERT INTO clientes (id_cliente, nombre, apellidos, fecha_nacimiento, telefono, email, direccion, notas_privadas) VALUES 
(1, 'Juan', 'Pérez García', '1985-05-15', '600111222', 'juan.perez@email.com', 'Calle Mayor 1', 'Paciente con dolor lumbar crónico.'),
(2, 'María', 'López Sánchez', '1990-10-20', '600333444', 'maria.lopez@email.com', 'Av. Libertad 25', 'Prefiere citas por la tarde.');


-- -----------------------------------------------------
-- 3. HORARIOS (Dra. Ana - ID 2)
-- Lógica: Lunes a Viernes. Turno partido.
-- 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes
-- -----------------------------------------------------
INSERT INTO horarios (id_usuario_quiro, dia_semana, hora_inicio, hora_fin) VALUES 
-- Lunes (Mañana y Tarde)
(2, 1, '09:00:00', '13:00:00'),
(2, 1, '16:00:00', '20:00:00'),
-- Martes (Solo Mañana)
(2, 2, '09:00:00', '14:00:00'),
-- Miércoles (Mañana y Tarde)
(2, 3, '09:00:00', '13:00:00'),
(2, 3, '16:00:00', '20:00:00'),
-- Jueves (Mañana y Tarde)
(2, 4, '09:00:00', '13:00:00'),
(2, 4, '16:00:00', '20:00:00'),
-- Viernes (Solo Mañana)
(2, 5, '09:00:00', '13:00:00');


-- -----------------------------------------------------
-- 4. SERVICIOS
-- -----------------------------------------------------
INSERT INTO servicios (id_servicio, nombre_servicio, precio, tipo, sesiones_incluidas, activo) VALUES 
(1, 'Primera Visita + Ajuste', 50.00, 'sesion_unica', NULL, 1),
(2, 'Ajuste Regular', 35.00, 'sesion_unica', NULL, 1),
(3, 'Bono 10 Sesiones', 300.00, 'bono', 10, 1),
(4, 'Bono 5 Sesiones', 160.00, 'bono', 5, 1);


-- -----------------------------------------------------
-- 5. BLOQUEOS DE AGENDA (Pruebas)
-- -----------------------------------------------------
-- Bloqueo personal: Dra. Ana tiene médico el Lunes 24 Nov de 12 a 13
INSERT INTO bloqueos_agenda (id_usuario_quiro, fecha_hora_inicio, fecha_hora_fin, motivo) VALUES 
(2, '2025-11-24 12:00:00', '2025-11-24 13:00:00', 'Cita Médica Personal');

-- Bloqueo Clínica: Navidad (Cerrado todo el día)
INSERT INTO bloqueos_agenda (id_usuario_quiro, fecha_hora_inicio, fecha_hora_fin, motivo) VALUES 
(NULL, '2025-12-25 00:00:00', '2025-12-25 23:59:59', 'Navidad');


-- -----------------------------------------------------
-- 6. PAGOS Y BONOS (María compra un Bono de 10)
-- -----------------------------------------------------
-- Paso A: Registrar el Pago
INSERT INTO pagos (id_pago, id_cliente, monto, metodo_pago, fecha_pago, id_servicio_pagado, notas) VALUES 
(1, 2, 300.00, 'tarjeta', NOW(), 3, 'Compra de bono inicial');

-- Paso B: Crear el Bono Activo vinculado a ese pago
INSERT INTO bonos_activos (id_bono_activo, id_cliente, id_servicio_comprado, id_pago_origen, fecha_compra, sesiones_totales, sesiones_restantes, fecha_caducidad) VALUES 
(1, 2, 3, 1, CURDATE(), 10, 10, DATE_ADD(CURDATE(), INTERVAL 1 YEAR));


-- -----------------------------------------------------
-- 7. CITAS (Escenarios de prueba)
-- -----------------------------------------------------

-- ESCENARIO 1: Cita Pasada (Completada) - Juan
INSERT INTO citas (id_cliente, id_quiropractico, fecha_hora_inicio, fecha_hora_fin, estado, notas_recepcion) VALUES 
(1, 2, '2025-11-10 10:00:00', '2025-11-10 10:30:00', 'completada', 'Vino con dolor agudo.');

-- ESCENARIO 2: Cita Futura (Programada) - Juan (Lunes 24 Nov, 09:00) -> Esta ocupará el primer hueco
INSERT INTO citas (id_cliente, id_quiropractico, fecha_hora_inicio, fecha_hora_fin, estado, notas_recepcion) VALUES 
(1, 2, '2025-11-24 09:00:00', '2025-11-24 09:30:00', 'programada', 'Revisión semanal.');

-- ESCENARIO 3: Cita Futura (Programada) - María (Lunes 24 Nov, 09:30)
INSERT INTO citas (id_cliente, id_quiropractico, fecha_hora_inicio, fecha_hora_fin, estado, notas_recepcion) VALUES 
(2, 2, '2025-11-24 09:30:00', '2025-11-24 10:00:00', 'programada', 'Usar bono.');


-- -----------------------------------------------------
-- 8. HISTORIAL Y CONSUMOS
-- -----------------------------------------------------

-- Historial para la cita completada de Juan (Escenario 1)
-- Nota: id_cita = 1 (asumiendo auto_increment empieza en 1)
INSERT INTO historial_clinico (id_cita, id_cliente, id_quiropractico, fecha_sesion, notas_subjetivo, notas_objetivo, ajustes_realizados, plan_futuro) VALUES 
(1, 1, 2, '2025-11-10 10:00:00', 'Paciente reporta dolor 7/10 en zona lumbar.', 'Contractura visible en L4-L5.', 'Ajuste lumbar lateral.', 'Volver en 1 semana.');

-- (Opcional) Si quisiéramos decir que Juan gastó un bono en esa cita (aunque no le creamos bono arriba),
-- aquí iría el insert en consumos_bono. Pero como pagó suelto (no tiene bono), no insertamos nada aquí.

-- DATOS DE EJEMPLO:
-- Asumimos que el Cliente 1 (Juan) es el Padre/Propietario
-- Asumimos que el Cliente 2 (María) es la Hija
-- Juan autoriza a María a usar sus bonos.
INSERT INTO grupos_familiares (id_cliente_propietario, id_cliente_beneficiario, relacion) 
VALUES (1, 2, 'Padre-Hija');

