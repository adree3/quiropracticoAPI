package com.example.quiropracticoapi.model.enums;

public enum TipoAccion {
    CREAR,
    EDITAR,
    ELIMINAR_LOGICO,
    REACTIVAR,
    ELIMINAR_FISICO,
    LOGIN,
    VENTA,
    BLOQUEADO,
    CONSUMO,
    UNLOCK,
    NOTIFICACION,
    ERROR,
    DESHACER // Historico: ya no se generan nuevas acciones de este tipo
}