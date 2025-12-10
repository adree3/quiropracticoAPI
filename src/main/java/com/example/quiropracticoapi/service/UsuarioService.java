package com.example.quiropracticoapi.service;

import com.example.quiropracticoapi.dto.UsuarioDto;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {
    Page<UsuarioDto> getAllUsuarios(Boolean activo ,Pageable pageable);
    UsuarioDto createUser(RegisterRequest request);
    UsuarioDto updateUser(Integer id, RegisterRequest request);
    void deleteUser(Integer id);
    void recoverUser(Integer id);
    void unlockUser(Integer id);
}
