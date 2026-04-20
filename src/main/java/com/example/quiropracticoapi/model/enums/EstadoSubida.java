package com.example.quiropracticoapi.model.enums;

public enum EstadoSubida {
    /** Registro creado en BD, subida a R2 aún no completada */
    PENDIENTE,
    /** Archivo subido correctamente a R2 y path guardado en BD */
    ACTIVO,
    /** Ocurrió un error durante la subida. El archivo puede o no estar en R2.
     *  Ver campo error_descripcion para diagnóstico. NUNCA se elimina de R2. */
    ERROR_SUBIDA,
    /** Archivo eliminado y desactivado. */
    ELIMINADO
}
