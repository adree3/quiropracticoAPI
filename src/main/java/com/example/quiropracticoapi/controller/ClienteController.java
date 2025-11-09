package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Gestión de Clientes", description = "API para el CRUD de clientes")
public class ClienteController {
    private final ClienteService clienteService;

    @Autowired
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Obtener todos los clientes", description = "Devuelve una lista de todos los clientes en la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes encontrados exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<ClienteDto>> getAllClientes() {
        List<ClienteDto> clientes = clienteService.getAllClientes();
        return ResponseEntity.ok(clientes);
    }

    @Operation(summary = "Obtener un cliente por su ID", description = "Devuelve un cliente específico buscado por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado (ResourceNotFoundException)")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDto> getClienteById(@PathVariable Integer id) {
        ClienteDto cliente = clienteService.getClienteById(id);
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Crear un nuevo cliente", description = "Registra un nuevo cliente en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (error de validación, ej. email incorrecto)")
    })
    @PostMapping
    public ResponseEntity<ClienteDto> createCliente(@Valid @RequestBody ClienteRequestDto clienteRequestDto) {
        ClienteDto clienteCreado = clienteService.createCliente(clienteRequestDto);
        return new ResponseEntity<>(clienteCreado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un cliente existente", description = "Actualiza los datos de un cliente usando su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (error de validación)"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteDto> updateCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteRequestDto clienteRequestDto) {

        ClienteDto clienteActualizado = clienteService.updateCliente(id, clienteRequestDto);
        return ResponseEntity.ok(clienteActualizado);
    }

    @Operation(summary = "Eliminar un cliente", description = "Elimina un cliente del sistema usando su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCliente(@PathVariable Integer id) {
        clienteService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar clientes por apellido", description = "Devuelve una lista de clientes cuyo apellido contenga el texto buscado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda completada (puede devolver lista vacía)")
    })
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteDto>> searchClientes(
            @RequestParam("apellidos") String textoApellidos) {

        List<ClienteDto> clientes = clienteService.searchClientesByApellidos(textoApellidos);
        return ResponseEntity.ok(clientes);
    }
}
