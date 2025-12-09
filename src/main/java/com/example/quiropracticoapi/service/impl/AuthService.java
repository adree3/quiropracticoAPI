package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.auth.AuthResponse;
import com.example.quiropracticoapi.dto.auth.LoginRequest;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
// BORRAMOS EL IMPORT DE UsernamePasswordToken PARA EVITAR ERRORES FANTASMA
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        user.setIntentosFallidos(0);
        user.setCuentaBloqueada(false);

        usuarioRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .rol(user.getRol().name())
                .nombre(user.getNombreCompleto())
                .build();
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest request) {
        Usuario user = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (user.isCuentaBloqueada()) {
            throw new LockedException("Cuenta bloqueada por seguridad. Contacte con un administrador.");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            if (user.getIntentosFallidos() > 0) {
                user.setIntentosFallidos(0);
                usuarioRepository.save(user);
            }

            String jwtToken = jwtService.generateToken(user);
            return AuthResponse.builder()
                    .token(jwtToken)
                    .rol(user.getRol().name())
                    .nombre(user.getNombreCompleto())
                    .build();

        } catch (BadCredentialsException e) {
            int nuevosIntentos = user.getIntentosFallidos() + 1;
            user.setIntentosFallidos(nuevosIntentos);

            if (nuevosIntentos >= 5) {
                user.setCuentaBloqueada(true);
                usuarioRepository.save(user);
                throw new LockedException("Has superado el límite de 5 intentos. Tu cuenta ha sido BLOQUEADA.");
            }
            usuarioRepository.save(user);
            throw new BadCredentialsException("Credenciales incorrectas. Intentos restantes: " + (5 - nuevosIntentos));
        }
    }
}