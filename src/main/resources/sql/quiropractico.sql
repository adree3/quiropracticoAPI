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
