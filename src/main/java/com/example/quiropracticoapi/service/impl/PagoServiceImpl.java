package com.example.quiropracticoapi.service.impl;
import com.example.quiropracticoapi.dto.BalanceDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.PagoResponseDto;
import com.example.quiropracticoapi.dto.PagosKpiDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Pago;
import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.MetodoPago;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.PagoRepository;
import com.example.quiropracticoapi.repository.ServicioRepository;
import com.example.quiropracticoapi.service.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServiceImpl implements PagoService {
    private final PagoRepository pagoRepository;
    private final BonoActivoRepository bonoActivoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicioRepository servicioRepository;
    private final AuditoriaServiceImpl auditoriaServiceImpl;

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, 
                           BonoActivoRepository bonoActivoRepository, 
                           ClienteRepository clienteRepository, 
                           ServicioRepository servicioRepository, 
                           AuditoriaServiceImpl auditoriaServiceImpl) {
        this.pagoRepository = pagoRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.clienteRepository = clienteRepository;
        this.servicioRepository = servicioRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponseDto> getPagos(LocalDateTime fechaInicio, LocalDateTime fechaFin, Boolean pagado, String search, Pageable pageable) {
        Page<Pago> paginaResultados = pagoRepository.findAllWithFilters(fechaInicio, fechaFin, pagado, search, pageable);
        return paginaResultados.map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponseDto> getPagos(LocalDateTime inicio, LocalDateTime fin, boolean pagado, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        return getPagos(inicio, fin, pagado, search, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDto> getPagosCliente(Integer idCliente) {
        return pagoRepository.findByClienteIdCliente(idCliente)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagosKpiDto getKpis(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // 1. Total Cobrado y Conteo de Ventas en Rango
        Double totalCobrado = pagoRepository.sumTotalCobrado(fechaInicio, fechaFin);
        Long cantidadVentas = pagoRepository.countTotalCobrado(fechaInicio, fechaFin);

        // 2. Efectivo en Rango Dinámico
        Double totalEfectivo = pagoRepository.sumEfectivoRango(fechaInicio, fechaFin);
        Long cantidadEfectivo = pagoRepository.countEfectivoRango(fechaInicio, fechaFin);

        // 3. Ventas en Bonos en Rango
        Double totalBonos = pagoRepository.sumVentasBonos(fechaInicio, fechaFin);
        Long cantidadBonos = pagoRepository.countVentasBonos(fechaInicio, fechaFin);
        Double porcentajeBonos = (totalCobrado != null && totalCobrado > 0)
                ? (totalBonos / totalCobrado) * 100.0
                : 0.0;

        // 4. Deuda Pendiente en Rango Dinámico
        Double totalPendiente = pagoRepository.sumDeudaPendiente(fechaInicio, fechaFin);
        Long cantidadPendientes = pagoRepository.countDeudaPendiente(fechaInicio, fechaFin);

        return PagosKpiDto.builder()
                .totalCobrado(totalCobrado != null ? totalCobrado : 0.0)
                .cantidadVentas(cantidadVentas != null ? cantidadVentas : 0L)
                .totalEfectivo(totalEfectivo != null ? totalEfectivo : 0.0)
                .cantidadEfectivo(cantidadEfectivo != null ? cantidadEfectivo : 0L)
                .totalBonos(totalBonos != null ? totalBonos : 0.0)
                .cantidadBonos(cantidadBonos != null ? cantidadBonos : 0L)
                .porcentajeBonos(Math.round(porcentajeBonos * 10.0) / 10.0)
                .totalPendiente(totalPendiente != null ? totalPendiente : 0.0)
                .cantidadPendientes(cantidadPendientes != null ? cantidadPendientes : 0L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceDto getBalance(LocalDateTime inicio, LocalDateTime fin) {
        PagosKpiDto kpis = getKpis(inicio, fin);
        return new BalanceDto(kpis.getTotalCobrado(), kpis.getTotalPendiente());
    }

    @Override
    @Transactional
    public void registrarVentaBono(VentaBonoRequestDto request) {
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Servicio servicio = servicioRepository.findById(request.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setMonto(servicio.getPrecio());
        pago.setServicioPagado(servicio);
        pago.setFechaCreacion(LocalDateTime.now());
        try {
            pago.setMetodoPago(MetodoPago.valueOf(request.getMetodoPago().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pago inválido: " + request.getMetodoPago());
        }

        if (request.getPagado() != null) {
            pago.setPagado(request.getPagado());
        } else {
            pago.setPagado(request.getMetodoPago().equalsIgnoreCase("efectivo"));
        }

        Pago pagoGuardado = pagoRepository.save(pago);

        // Crear el Bono Activo
        BonoActivo bono = new BonoActivo();
        bono.setCliente(cliente);
        bono.setServicioComprado(servicio);
        bono.setPagoOrigen(pagoGuardado);
        bono.setFechaCompra(LocalDate.now());

        int sesiones;
        if (servicio.getSesionesIncluidas() != null && servicio.getSesionesIncluidas() > 0) {
            sesiones = servicio.getSesionesIncluidas();
        } else {
            sesiones = 1;
        }

        bono.setSesionesTotales(sesiones);
        bono.setSesionesRestantes(sesiones);
        bonoActivoRepository.save(bono);

        String estadoPago = pagoGuardado.isPagado() ? "COBRADO" : "PENDIENTE DE PAGO";
        auditoriaServiceImpl.registrarAccion(
                TipoAccion.VENTA,
                "PAGO",
                pagoGuardado.getIdPago().toString(),
                "Venta registrada: " + servicio.getNombreServicio() +
                        ". Cliente: " + cliente.getNombre() + " " + cliente.getApellidos() +
                        ". Monto: " + pago.getMonto() + "€. Estado: " + estadoPago +
                        ". Método: " + pago.getMetodoPago()
        );
    }

    @Override
    @Transactional
    public void confirmarPago(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        boolean estabaPagado = pago.isPagado();
        if (!estabaPagado) {
            pago.setPagado(true);
            pago.setFechaCreacion(LocalDateTime.now());
            pagoRepository.save(pago);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.EDITAR,
                    "PAGO",
                    idPago.toString(),
                    "Cobro confirmado: " + pago.getMonto() + "€ - " + pago.getCliente().getNombre()
            );
        }
    }

    @Override
    @Transactional
    public void pendientePago(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (pago.isPagado()) {
            pago.setPagado(false);
            pago.setFechaCreacion(null);
            pagoRepository.save(pago);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.EDITAR,
                    "PAGO",
                    idPago.toString(),
                    "[REVERTIDO] Cobro marcado como pendiente: " + pago.getMonto() + "€ - " + pago.getCliente().getNombre()
            );
        }
    }

    private PagoResponseDto toResponseDto(Pago p) {
        PagoResponseDto dto = new PagoResponseDto();
        dto.setIdPago(p.getIdPago());
        dto.setIdCliente(p.getCliente() != null ? p.getCliente().getIdCliente() : null);
        dto.setNombreCliente(p.getCliente() != null 
                ? (p.getCliente().getNombre() + " " + (p.getCliente().getApellidos() != null ? p.getCliente().getApellidos() : "")).trim() 
                : "Sin Cliente");
        dto.setConcepto(p.getServicioPagado() != null 
                ? p.getServicioPagado().getNombreServicio() 
                : (p.getNotas() != null ? p.getNotas() : "Cobro"));
        dto.setMonto(p.getMonto());
        dto.setMetodoPago(p.getMetodoPago() != null ? p.getMetodoPago().name() : "");
        dto.setFechaPago(p.getFechaCreacion());
        dto.setPagado(p.isPagado());
        return dto;
    }

    private PagoDto toDto(Pago p) {
        PagoDto dto = new PagoDto();
        dto.setIdPago(p.getIdPago());
        dto.setIdCliente(p.getCliente() != null ? p.getCliente().getIdCliente() : null);
        dto.setNombreCliente(p.getCliente() != null 
                ? (p.getCliente().getNombre() + " " + (p.getCliente().getApellidos() != null ? p.getCliente().getApellidos() : "")).trim() 
                : "Sin Cliente");
        dto.setConcepto(p.getServicioPagado() != null 
                ? p.getServicioPagado().getNombreServicio() 
                : (p.getNotas() != null ? p.getNotas() : "Cobro"));
        dto.setMonto(p.getMonto());
        dto.setMetodoPago(p.getMetodoPago() != null ? p.getMetodoPago().name() : "");
        dto.setFechaPago(p.getFechaCreacion());
        dto.setPagado(p.isPagado());
        return dto;
    }
}
