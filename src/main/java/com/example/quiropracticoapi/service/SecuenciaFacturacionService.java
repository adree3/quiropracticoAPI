package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.model.Clinica;

public interface SecuenciaFacturacionService {
    String generarSiguienteNumero(Clinica clinica);
}
