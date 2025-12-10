package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.UsuarioDto;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
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

    // GET paginado
    @GetMapping
    public ResponseEntity<Page<UsuarioDto>> getAll(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(usuarioService.getAllUsuarios(activo, PageRequest.of(page, size)));
    }

    // POST Crear
    @PostMapping
    public ResponseEntity<UsuarioDto> create(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(usuarioService.createUser(request));
    }

    // PUT Editar
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDto> update(@PathVariable Integer id, @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(usuarioService.updateUser(id, request));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        usuarioService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // RECUPERAR
    @PutMapping("/{id}/recuperar")
    public ResponseEntity<Void> recover(@PathVariable Integer id) {
        usuarioService.recoverUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/quiros")
    public ResponseEntity<List<UsuarioDto>> getQuiropracticos() {
        return ResponseEntity.ok(
                usuarioRepository.findByRol(Rol.quiropráctico)
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

    // DESBLOQUEAR
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<Void> unlock(@PathVariable Integer id) {
        usuarioService.unlockUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bloqueados/count")
    public ResponseEntity<Long> countBlocked() {
        return ResponseEntity.ok(usuarioRepository.countByCuentaBloqueadaTrue());
    }
}
