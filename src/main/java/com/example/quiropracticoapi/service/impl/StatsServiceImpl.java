package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.ChartDataDto;
import com.example.quiropracticoapi.dto.DashboardStatsDto;
import com.example.quiropracticoapi.model.enums.EstadoCita;
import com.example.quiropracticoapi.repository.CitaRepository;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.PagoRepository;
import com.example.quiropracticoapi.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatsServiceImpl implements StatsService {

    private final PagoRepository pagoRepo;
    private final CitaRepository citaRepo;
    private final ClienteRepository clienteRepo;

    @Autowired
    public StatsServiceImpl(PagoRepository pagoRepo, CitaRepository citaRepo, ClienteRepository clienteRepo) {
        this.pagoRepo = pagoRepo;
        this.citaRepo = citaRepo;
        this.clienteRepo = clienteRepo;
    }

    @Override
    public DashboardStatsDto getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();


        LocalDateTime inicioHoy = now.toLocalDate().atStartOfDay();
        LocalDateTime finHoy = now.toLocalDate().atTime(23, 59, 59);

        LocalDateTime inicioMes = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime finMes = now.withDayOfMonth(now.getMonth().length(now.toLocalDate().isLeapYear())).toLocalDate().atTime(23, 59, 59);

        LocalDateTime inicioSemana = now.minusDays(6).toLocalDate().atStartOfDay();
        LocalDateTime finSemana = now.toLocalDate().atTime(23, 59, 59);

        java.util.List<com.example.quiropracticoapi.dto.ChartDataProjection> rawGrafica = 
            pagoRepo.sumTotalCobradoPorDia(inicioSemana, finSemana);

        List<ChartDataDto> grafica = new ArrayList<>();
        // Mapeamos los resultados a DTOs para el front
        for (com.example.quiropracticoapi.dto.ChartDataProjection p : rawGrafica) {
            // El label viene como YYYY-MM-DD del SQL, lo convertimos a algo legible (ej: Lun)
            java.time.LocalDate date = java.time.LocalDate.parse(p.getLabel());
            String shortLabel = date.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.SHORT, new java.util.Locale("es", "ES")
            );
            grafica.add(new ChartDataDto(shortLabel, p.getValue() != null ? p.getValue() : 0.0));
        }
        return DashboardStatsDto.builder()
                // DINERO
                .ingresosHoy(pagoRepo.sumTotalCobradoEnRango(inicioHoy, finHoy))
                .ingresosMes(pagoRepo.sumTotalCobradoEnRango(inicioMes, finMes))

                // CITAS
                .citasHoyTotal(citaRepo.countByFechaHoraInicioBetween(inicioHoy, finHoy))
                .citasHoyPendientes(citaRepo.countByFechaHoraInicioBetweenAndEstado(inicioHoy, finHoy, EstadoCita.programada))

                // CLIENTES
                .nuevosClientesMes(clienteRepo.countByFechaAltaBetween(inicioMes, finMes))
                .totalClientesActivos(clienteRepo.countByActivoTrue())
                .graficaIngresos(grafica)
                .build();
    }
}