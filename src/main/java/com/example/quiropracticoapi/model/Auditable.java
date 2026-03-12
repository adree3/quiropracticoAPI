package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.enums.TipoAccion;

/**
 * Contrato para entidades que quieren proporcionar un resumen legible para los logs de auditoría.
 *
 * La tabla de auditoría muestra este resumen en la columna "Detalle" (texto para el usuario).
 * El JSON técnico completo de la entidad se guarda en el campo "detalles" y se accede
 * mediante el botón "Resumen" (para administradores o soporte técnico).
 */
public interface Auditable {

    /**
     * Genera un texto corto y legible que describe el estado actual de la entidad
     * en el contexto de la acción que se está auditando.
     *
     * Ejemplo: "Cita #177 | Paciente: Adrian García | Estado: cancelada"
     *
     * @param accion La acción que se está registrando (CREAR, EDITAR, ELIMINAR_LOGICO, etc.)
     * @return texto descriptivo de máx ~200 caracteres, legible para el usuario final
     */
    String toResumen(TipoAccion accion);
}
