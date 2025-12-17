package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.UsuarioDto;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import com.example.quiropracticoapi.exception.ResourceNotFoundException;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import com.example.quiropracticoapi.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Autowired
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public Page<UsuarioDto> getAllUsuarios(Boolean activo, Pageable pageable) {
        Page<Usuario> page;
        if (activo == null) {
            page = usuarioRepository.findAll(pageable);
        } else {
            page = usuarioRepository.findByActivo(activo, pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    public UsuarioDto createUser(RegisterRequest request) {
        Usuario user = new Usuario();
        user.setUsername(request.getUsername());
        user.setNombreCompleto(request.getNombreCompleto());
        user.setRol(request.getRol());
        user.setActivo(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario guardado = usuarioRepository.save(user);

        auditoriaService.registrarAccion(
                TipoAccion.CREAR,
                "USUARIO",
                guardado.getUsername(),
                "Alta de empleado. Nombre: " + guardado.getNombreCompleto() + ". Rol asignado: " + guardado.getRol()
        );

        return toDto(guardado);
    }

    @Override
    public UsuarioDto updateUser(Integer id, RegisterRequest request) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String cambios = "";
        if (!user.getRol().equals(request.getRol())) {
            cambios += "Rol cambiado de " + user.getRol() + " a " + request.getRol() + ". ";
        }

        boolean cambioPassword = false;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            cambioPassword = true;
            cambios += "Contraseña modificada. ";
        }
        user.setNombreCompleto(request.getNombreCompleto());
        user.setRol(request.getRol());
        Usuario guardado = usuarioRepository.save(user);

        auditoriaService.registrarAccion(
                TipoAccion.EDITAR,
                "USUARIO",
                guardado.getUsername(),
                "Edición de perfil. " + cambios
        );

        return toDto(guardado);
    }

    @Override
    public void deleteUser(Integer id) {
        Usuario user = usuarioRepository.findById(id).orElseThrow();
        user.setActivo(false);
        usuarioRepository.save(user);
        auditoriaService.registrarAccion(
                TipoAccion.ELIMINAR_LOGICO,
                "USUARIO",
                user.getUsername(),
                "Empleado desactivado (enviado a papelera)."
        );
    }

    @Override
    public void recoverUser(Integer id) {
        Usuario user = usuarioRepository.findById(id).orElseThrow();
        user.setActivo(true);
        usuarioRepository.save(user);
        auditoriaService.registrarAccion(
                TipoAccion.REACTIVAR,
                "USUARIO",
                user.getUsername(),
                "Empleado reactivado (restaurado)."
        );
    }

    @Override
    public void unlockUser(Integer id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setCuentaBloqueada(false);
        user.setIntentosFallidos(0);
        usuarioRepository.save(user);

        auditoriaService.registrarAccion(
                TipoAccion.UNLOCK,
                "USUARIO",
                user.getUsername(),
                "Desbloqueo manual de cuenta por administrador. Reset de intentos fallidos."
        );
    }

    private UsuarioDto toDto(Usuario u) {
        UsuarioDto dto = new UsuarioDto();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setUsername(u.getUsername());
        dto.setRol(u.getRol());
        dto.setActivo(u.isActivo());
        dto.setCuentaBloqueada(u.isCuentaBloqueada());
        return dto;
    }
}
