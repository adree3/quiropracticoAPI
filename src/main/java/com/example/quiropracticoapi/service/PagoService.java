package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.BalanceDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoService {

    void registrarVentaBono(VentaBonoRequestDto request);

    Page<PagoDto> getPagos(LocalDateTime inicio, LocalDateTime fin, boolean pagado, String search, int page, int size);

    BalanceDto getBalance(LocalDateTime inicio, LocalDateTime fin);

    void confirmarPago(Integer idPago);
}
