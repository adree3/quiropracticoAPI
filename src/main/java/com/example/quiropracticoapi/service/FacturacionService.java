package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Clinica;
import com.example.quiropracticoapi.model.Factura;
import com.example.quiropracticoapi.model.enums.TipoFactura;

import java.util.List;

public interface FacturacionService {
    Factura emitirFacturaPorPagos(List<Integer> idsPagos, Cliente cliente, Clinica clinica, TipoFactura tipoFactura);

    String obtenerUrlFacturaPorPago(Integer idPago);
}
