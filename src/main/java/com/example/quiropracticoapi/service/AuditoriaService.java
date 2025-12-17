package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.model.enums.TipoAccion;

public interface AuditoriaService {

    /**
     * Registra una accion en segundo plano
     * @param accion (crear, editar, borrar, venta)
     * @param entidad sobre quien lo hizo (usuario, cita...)
     * @param idEntidad identificador de la entidad
     * @param detalles detalles extra
     */
    void registrarAccion(TipoAccion accion, String entidad, String idEntidad, String detalles);


}
