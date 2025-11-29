package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.auth.AuthResponse;
import com.example.quiropracticoapi.dto.auth.LoginRequest;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
// BORRAMOS EL IMPORT DE UsernamePasswordToken PARA EVITAR ERRORES FANTASMA
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        Usuario user = new Usuario();
        user.setUsername(request.getUsername());
        user.setNombreCompleto(request.getNombreCompleto());
        user.setRol(request.getRol());
        user.setActivo(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        usuarioRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .rol(user.getRol().name())
                .nombre(user.getNombreCompleto())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        Usuario user = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .rol(user.getRol().name())
                .nombre(user.getNombreCompleto())
                .build();
    }
}