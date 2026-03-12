package com.example.quiropracticoapi.model;

/**
 * Contrato para entidades que implementan borrado lógico.
 * El AuditEventListener la usará para detectar genéricamente si una actualización
 * debe registrarse como ELIMINAR_LOGICO en lugar de EDITAR.
 *
 * Implementar esta interfaz en cualquier entidad que tenga algún tipo de borrado
 * lógico (campo 'activo', 'estado', 'eliminado', etc.) garantiza que la auditoría
 * automática siempre lo capture correctamente, sin cadenas hardcodeadas.
 */
public interface SoftDeletable {

    /**
     * @return true si la entidad está en estado de "eliminada lógicamente".
     *         La entidad implementadora decide la lógica concreta.
     */
    boolean isEliminadoLogico();
}
