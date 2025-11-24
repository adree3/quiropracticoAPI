package com.example.quiropracticoapi.controller;

import com.example.quiropracticoapi.dto.UsuarioDto;
import com.example.quiropracticoapi.model.enums.Rol;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Gestión de Usuarios", description = "Listados de personal")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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
}
