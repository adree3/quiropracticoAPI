package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.ClienteDto;
import com.example.quiropracticoapi.dto.ClienteRequestDto;
import com.example.quiropracticoapi.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClienteService {
    /**
     * Obtiene una lista de todos los clientes.
     * @return Lista de DTOs de clientes.
     */
    Page<ClienteDto> getAllClientes(Boolean activo, Pageable pageable);

    /**
     * Busca un cliente por su ID.
     * @param id El ID del cliente.
     * @return El DTO de cliente encontrado.
     * @throws com.example.quiropracticoapi.exception.ResourceNotFoundException si no se encuentra.
     */
    ClienteDto getClienteById(Integer id);

    /**
     * Crea un nuevo cliente en la base de datos.
     * @param clienteRequestDto El objeto cliente a crear.
     * @return El cliente guardado (con su ID asignado).
     */
    ClienteDto createCliente(ClienteRequestDto clienteRequestDto);

    /**
     * Actualiza un cliente existente.
     * @param id El ID del cliente a actualizar.
     * @param clienteRequestDto El objeto con los nuevos datos.
     * @param undo Indica si es una accion de deshacer.
     * @return El cliente actualizado.
     * @throws com.example.quiropracticoapi.exception.ResourceNotFoundException si no se encuentra.
     */
    ClienteDto updateCliente(Integer id, ClienteRequestDto clienteRequestDto, boolean undo);

    /**
     * Elimina un cliente por su ID.
     * @param id El ID del cliente a eliminar.
     * @param undo Indica si es una accion de deshacer.
     * @throws com.example.quiropracticoapi.exception.ResourceNotFoundException si no se encuentra.
     */
    void deleteCliente(Integer id, boolean undo);

    /**
     * Busca clientes cuyos apellidos contengan el textoBuscado.
     * @param texto El texto a buscar (ej. "Garc").
     * @return Lista de clientes que coinciden.
     */
    List<ClienteDto> searchClientesList(String texto);

    /**
     * Busca clientes por el filtro del texto que recibe
     * @param texto por el cual se filtra
     * @param pageable para el page
     * @return un page de clientes filtrados
     */
    Page<ClienteDto> searchClientesPaged(String texto, Pageable pageable);

    /**
     * Busca clientes con filtros opcionales flexibles (unifica getAllClientes y searchClientes)
     * @param activo true para activos, false para inactivos, null para todos
     * @param texto búsqueda global por nombre/apellido/teléfono (opcional)
     * @param lastActivityDays número de días para filtrar por última actividad (opcional)
     * @param pageable paginación y ordenamiento
     * @return page de clientes que cumplen los criterios, con información extendida
     */
    Page<ClienteDto> findClientesWithFilters(
        Boolean activo,
        String texto,
        Integer lastActivityDays,
        Pageable pageable
    );

    /**
     * Vincula dos clientes en un grupo familiar.
     * @param idPropietario El dueño de los bonos.
     * @param idBeneficiario Quien podrá usarlos.
     * @param relacion Texto descriptivo (ej. "Hijo", "Pareja").
     */
    void agregarFamiliar(Integer idPropietario, Integer idBeneficiario, String relacion, boolean undo, List<Integer> idsCitasARestaurar);

    /**
     * Cambia el cliente a activo
     * @param id identificador cliente
     * @param undo Indica si es una accion de deshacer.
     */
    void recoverCliente(Integer id, boolean undo);

    /**
     * Elimina a un familiar.
     * @param idGrupo identificador del grupo familiar
     */
    void deleteFamiliar(Integer idGrupo);

}
