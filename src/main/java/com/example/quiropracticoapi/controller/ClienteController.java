package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.dto.FamiliarDto;
import com.example.quiropracticoapi.repository.GrupoFamiliarRepository;
import com.example.quiropracticoapi.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Gestión de Clientes", description = "API para el CRUD de clientes")
public class ClienteController {
    private final ClienteService clienteService;
    private final GrupoFamiliarRepository grupoFamiliarRepository;


    @Autowired
    public ClienteController(ClienteService clienteService, GrupoFamiliarRepository grupoFamiliarRepository) {
        this.clienteService = clienteService;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
    }

    @Operation(summary = "Obtener clientes paginados", description = "Devuelve una lista paginada de clientes.")
    @GetMapping
    public ResponseEntity<Page<ClienteDto>> getAllClientes(
            @RequestParam(defaultValue = "true") Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idCliente") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClienteDto> clientesPage = clienteService.getAllClientes(activo, pageable);
        return ResponseEntity.ok(clientesPage);
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

    // POST /api/clientes/1/familiares?idBeneficiario=2&relacion=Hijo
    @PostMapping("/{idPropietario}/familiares")
    public ResponseEntity<Void> agregarFamiliar(
            @PathVariable Integer idPropietario,
            @RequestParam Integer idBeneficiario,
            @RequestParam String relacion
    ) {
        clienteService.agregarFamiliar(idPropietario, idBeneficiario, relacion);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtiene a la gente que ha autorizado el cliente a usar sus bonos
     * @param id identificador cliente
     * @return lista de familiares del cliente
     */
    @GetMapping("/{id}/familiares")
    public ResponseEntity<List<FamiliarDto>> getFamiliares(@PathVariable Integer id) {

        return ResponseEntity.ok(
                grupoFamiliarRepository.findByPropietarioIdCliente(id).stream()
                        .map(g -> {
                            FamiliarDto dto = new FamiliarDto();
                            dto.setIdGrupo(g.getIdGrupo());
                            dto.setIdFamiliar(g.getBeneficiario().getIdCliente());
                            dto.setNombreCompleto(g.getBeneficiario().getNombre() + " " + g.getBeneficiario().getApellidos());
                            dto.setRelacion(g.getRelacion());
                            return dto;
                        })
                        .collect(Collectors.toList())
        );
    }

    @Operation(summary = "Buscar clientes por apellido", description = "Devuelve una lista de clientes cuyo apellido contenga el texto buscado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda completada (puede devolver lista vacía)")
    })
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteDto>> searchClientes(
            @RequestParam("texto") String texto) {

        List<ClienteDto> clientes = clienteService.searchClientesList(texto);
        return ResponseEntity.ok(clientes);
    }

    /**
     * Filtra a los clientes por el texto recibido
     * @param texto por el cual se filtra
     * @param page la página que empieza
     * @param size numero de clientes por page
     * @return page de clientes filtrados
     */
    @GetMapping("/buscar-complejo")
    public ResponseEntity<Page<ClienteDto>> searchClientes(
            @RequestParam("texto") String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ClienteDto> resultados = clienteService.searchClientesPaged(texto, pageable);

        return ResponseEntity.ok(resultados);
    }

    /**
     * Cambia al cliente a activo
     * @param id identificador del cliente
     * @return la respuesta de confirmacion
     */
    @PutMapping("/{id}/recuperar")
    public ResponseEntity<Void> recoverCliente(@PathVariable Integer id) {
        clienteService.recoverCliente(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina un familiar
     * @param idGrupo identificadior del grupo familiar
     * @return respuesta de eliminar un familiar
     */
    @DeleteMapping("/familiares/{idGrupo}")
    public ResponseEntity<Void> deleteFamiliar(@PathVariable Integer idGrupo) {
        clienteService.deleteFamiliar(idGrupo);
        return ResponseEntity.ok().build();
    }
}
