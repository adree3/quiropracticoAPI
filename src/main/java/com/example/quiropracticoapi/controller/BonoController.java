package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.BonoDto;
import com.example.quiropracticoapi.dto.BonoSeleccionDto;
import com.example.quiropracticoapi.repository.BonoActivoRepository;
import com.example.quiropracticoapi.service.BonoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bonos")
@Tag(name = "Gestión de Bonos", description = "Consultar saldo y bonos activos")
public class BonoController {
    private final BonoActivoRepository bonoRepo;
    private final BonoService bonoService;

    @Autowired
    public BonoController(BonoActivoRepository bonoRepo, BonoService bonoService) {
        this.bonoRepo = bonoRepo;
        this.bonoService = bonoService;
    }

    /**
     * Obtiene los bonos que tenga el cliente
     * @param idCliente identificador del cliente
     * @return lista de bonos del cliente
     */
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<BonoDto>> getBonosCliente(@PathVariable Integer idCliente) {
        List<BonoDto> bonos = bonoRepo.findByClienteIdCliente(idCliente).stream()
                .map(b -> {
                    BonoDto dto = new BonoDto();
                    dto.setIdBonoActivo(b.getIdBonoActivo());
                    dto.setNombreServicio(b.getServicioComprado().getNombreServicio());
                    dto.setSesionesTotales(b.getSesionesTotales());
                    dto.setSesionesRestantes(b.getSesionesRestantes());
                    dto.setFechaCaducidad(b.getFechaCaducidad());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(bonos);
    }

    /**
     * Obtiene los bonos usados del cliente (suyos o de sus familiares)
     * @param idCliente identificador del cliente
     * @return lista de bonos que puede usar
     */
    @GetMapping("/disponibles/{idCliente}")
    public ResponseEntity<List<BonoSeleccionDto>> getBonosUsables(@PathVariable Integer idCliente) {
        return ResponseEntity.ok(bonoService.getBonosUsables(idCliente));
    }

}
