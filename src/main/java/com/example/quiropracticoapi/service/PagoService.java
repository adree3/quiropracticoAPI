package com.example.quiropracticoapi.service;
import com.example.quiropracticoapi.dto.BalanceDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.PagoResponseDto;
import com.example.quiropracticoapi.dto.PagosKpiDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoService {

    void registrarVentaBono(VentaBonoRequestDto request);

    Page<PagoResponseDto> getPagos(LocalDateTime fechaInicio, LocalDateTime fechaFin, Boolean pagado, String search, Pageable pageable);

    Page<PagoResponseDto> getPagos(LocalDateTime inicio, LocalDateTime fin, boolean pagado, String search, int page, int size);

    List<PagoDto> getPagosCliente(Integer idCliente);

    PagosKpiDto getKpis(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    BalanceDto getBalance(LocalDateTime inicio, LocalDateTime fin);

    void confirmarPago(Integer idPago);

    void pendientePago(Integer idPago);
}
