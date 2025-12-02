package com.example.quiropracticoapi.service.impl;

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

        // Guardamos el pago primero para tener su ID
        Pago pagoGuardado = pagoRepository.save(pago);

        // Crear el Bono Activo (Producto entregado)
        BonoActivo bono = new BonoActivo();
        bono.setCliente(cliente);
        bono.setServicioComprado(servicio);
        bono.setPagoOrigen(pagoGuardado);
        bono.setFechaCompra(LocalDate.now());

        int sesiones = servicio.getSesionesIncluidas() != null ? servicio.getSesionesIncluidas() : 0;
        bono.setSesionesTotales(sesiones);
        bono.setSesionesRestantes(sesiones);

        bonoActivoRepository.save(bono);
    }
}
