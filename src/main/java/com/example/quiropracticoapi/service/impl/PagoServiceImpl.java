package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.dto.PagoDto;
import com.example.quiropracticoapi.dto.VentaBonoRequestDto;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.BonoActivo;
import com.example.quiropracticoapi.model.Cliente;
import com.example.quiropracticoapi.model.Pago;
import com.example.quiropracticoapi.model.Servicio;
import com.example.quiropracticoapi.model.enums.MetodoPago;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.repository.ClienteRepository;
import com.example.quiropracticoapi.repository.PagoRepository;
import com.example.quiropracticoapi.repository.ServicioRepository;
import com.example.quiropracticoapi.service.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository, BonoActivoRepository bonoActivoRepository, ClienteRepository clienteRepository, ServicioRepository servicioRepository) {
        this.pagoRepository = pagoRepository;
        this.bonoActivoRepository = bonoActivoRepository;
        this.clienteRepository = clienteRepository;
        this.servicioRepository = servicioRepository;
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
        try {
            pago.setMetodoPago(MetodoPago.valueOf(request.getMetodoPago().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pago inválido: " + request.getMetodoPago());
        }

        if (request.getPagado() != null) {
            pago.setPagado(request.getPagado());
        } else {
            String metodo = request.getMetodoPago().toLowerCase();
            if (metodo.equals("efectivo")) {
                pago.setPagado(true);
            } else {
                pago.setPagado(false);
            }
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
    }

    @Override
    public List<PagoDto> getPagosEnRango(LocalDateTime inicio, LocalDateTime fin) {
        return pagoRepository.findByFechaPagoBetweenOrderByFechaPagoDesc(inicio, fin)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<PagoDto> getPagosPendientes() {
        return pagoRepository.findByPagadoFalseOrderByFechaPagoDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void confirmarPago(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        pago.setPagado(true);
        pagoRepository.save(pago);
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
