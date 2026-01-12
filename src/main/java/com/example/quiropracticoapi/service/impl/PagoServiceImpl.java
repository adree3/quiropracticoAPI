package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BalanceDto;
import com.example.quiropracticoapi.dto.PagoDto;
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
    public PagoServiceImpl(PagoRepository pagoRepository, BonoActivoRepository bonoActivoRepository, ClienteRepository clienteRepository, ServicioRepository servicioRepository, AuditoriaServiceImpl auditoriaServiceImpl) {
        this.pagoRepository = pagoRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.clienteRepository = clienteRepository;
        this.servicioRepository = servicioRepository;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    @Override
    public Page<PagoDto> getPagos(LocalDateTime inicio, LocalDateTime fin, boolean pagado, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaPago").descending());

        Page<Pago> paginaResultados;

        if (!pagado) {
            paginaResultados = pagoRepository.findPendientesWithSearch(search, pageable);
        } else {
            if (inicio == null) inicio = LocalDateTime.of(2000, 1, 1, 0, 0);
            if (fin == null) fin = LocalDateTime.now();

            paginaResultados = pagoRepository.findHistorialWithSearch(inicio, fin, search, pageable);
        }

        return paginaResultados.map(this::toDto);
    }

    @Override
    public BalanceDto getBalance(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null) inicio = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (fin == null) fin = LocalDateTime.now();

        Double cobrado = pagoRepository.sumTotalCobradoEnRango(inicio, fin);
        Double pendiente = pagoRepository.sumTotalPendienteGlobal();

        return new BalanceDto(cobrado, pendiente);
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
        pago.setFechaPago(LocalDateTime.now());
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

        // Guardamos el pago para tener su ID
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
        }else {
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
    public void confirmarPago(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        boolean estabaPagado = pago.isPagado();
        if (!estabaPagado) {
            pago.setPagado(true);
            pago.setFechaPago(LocalDateTime.now());
            pagoRepository.save(pago);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.EDITAR,
                    "PAGO",
                    idPago.toString(),
                    "Cobro confirmado: " + pago.getMonto() + "€ - " + pago.getCliente().getNombre()
            );
        }
    }

    private PagoDto toDto(Pago p) {
        PagoDto dto = new PagoDto();
        dto.setIdPago(p.getIdPago());
        dto.setNombreCliente(p.getCliente().getNombre() + " " + p.getCliente().getApellidos());
        dto.setConcepto(p.getServicioPagado() != null ? p.getServicioPagado().getNombreServicio() : "Cobro");
        dto.setMonto(p.getMonto());
        dto.setMetodoPago(p.getMetodoPago().name());
        dto.setFechaPago(p.getFechaPago());
        dto.setPagado(p.isPagado());
        return dto;
    }
}
