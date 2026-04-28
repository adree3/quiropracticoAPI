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
    void lockUser(Integer id);
    UsuarioDto getMe(String usernameOrId);
    UsuarioDto getMeById(Integer id);
    void updatePassword(String usernameOrId, String currentPassword, String newPassword);
    void updatePasswordById(Integer id, String currentPassword, String newPassword);
    
    /** Sube la foto a R2 y actualiza el usuario */
    String uploadFotoPerfil(Integer idUsuario, org.springframework.web.multipart.MultipartFile file);
    
    /** Descarga la foto en bytes para el proxy con caché estática */
    byte[] getFotoPerfil(Integer idUsuario);
}
