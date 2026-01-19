package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.UsuarioDto;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.Rol;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import com.example.quiropracticoapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Gestión de Usuarios", description = "Listados de personal")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Obtiene los usuarios paginados
     * @param activo para recibir los usuarios activo o no
     * @param page para la paginacion
     * @param size numero de la pagina
     * @return page de usuarios
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioDto>> getAll(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(usuarioService.getAllUsuarios(activo, PageRequest.of(page, size)));
    }

    /**
     * Crea un usuario con los datos recibidos
     * @param request datos para crear el usuario
     * @return el usuario creado
     */
    @PostMapping
    public ResponseEntity<UsuarioDto> create(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(usuarioService.createUser(request));
    }

    /**
     * Edita un usuario con los datos recibidos
     * @param id identificador del usuario
     * @param request datos para editar
     * @return el usuario editado
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDto> update(@PathVariable Integer id, @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(usuarioService.updateUser(id, request));
    }

    /**
     * Elimina un usuario logicamente
     * @param id identificador del usuario
     * @return respuesta de eliminar un usuario
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        usuarioService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Pone un usuario eliminado a activo
     * @param id identificador del usuario
     * @return respuesta de activar el usuario
     */
    @PutMapping("/{id}/recuperar")
    public ResponseEntity<Void> recover(@PathVariable Integer id) {
        usuarioService.recoverUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Devuelve todos los quiropracticos, activos o no
     * @return lista de ususarios
     */
    @GetMapping("/quiros")
    public ResponseEntity<List<UsuarioDto>> getQuiropracticos() {
        return ResponseEntity.ok()
                .body(usuarioRepository.findByRol(Rol.quiropráctico)
                    .stream()
                    .map(u -> {
                        UsuarioDto dto = new UsuarioDto();
                        dto.setIdUsuario(u.getIdUsuario());
                        dto.setNombreCompleto(u.getNombreCompleto());
                        dto.setUsername(u.getUsername());
                        return dto;
                    })
                    .collect(Collectors.toList())
        );
    }

    /**
     * Devuelve los usuarios quiropracticos activos
     * @return lista de usuarios
     */
    @GetMapping("/quiros-activos")
    public ResponseEntity<List<UsuarioDto>> getQuiropracticosActivos() {

        List<Usuario> quiros = usuarioRepository.findQuiropracticosActivos();

        List<UsuarioDto> dtos = quiros.stream()
                .map(usuario -> {
                    UsuarioDto dto = new UsuarioDto();
                    dto.setIdUsuario(usuario.getIdUsuario());
                    dto.setNombreCompleto(usuario.getNombreCompleto());
                    dto.setUsername(usuario.getUsername());
                    dto.setRol(usuario.getRol());
                    dto.setActivo(usuario.isActivo());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Desbloquea a un usuario
     * @param id identificador del usuario
     * @return resultado de desloquear el usuario
     */
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<Void> unlock(@PathVariable Integer id) {
        usuarioService.unlockUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Cuenta el numero de bloqueados que hay
     * @return numero de bloqueados
     */
    @GetMapping("/bloqueados/count")
    public ResponseEntity<Long> countBlocked() {
        return ResponseEntity.ok(usuarioRepository.countByCuentaBloqueadaTrueAndActivoTrue());
    }
}
