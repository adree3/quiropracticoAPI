package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.dto.auth.AuthResponse;
import com.example.quiropracticoapi.dto.auth.LoginRequest;
import com.example.quiropracticoapi.dto.auth.RegisterRequest;
import com.example.quiropracticoapi.model.Usuario;
import com.example.quiropracticoapi.model.enums.Rol;
import com.example.quiropracticoapi.model.enums.TipoAccion;
import com.example.quiropracticoapi.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditoriaServiceImpl auditoriaServiceImpl;

    // Rate Limiting: IP -> {intentos, ultimoIntento}
    private static class RateLimitInfo {
        int intentos;
        LocalDateTime ultimoIntento;
        RateLimitInfo(int intentos, LocalDateTime ultimoIntento) {
            this.intentos = intentos;
            this.ultimoIntento = ultimoIntento;
        }
    }
    private final Map<String, RateLimitInfo> loginAttemptsByIp = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final int BLOCK_TIME_MINUTES = 15;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, AuditoriaServiceImpl auditoriaServiceImpl) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditoriaServiceImpl = auditoriaServiceImpl;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty()) {
                // X-Forwarded-For puede contener una lista de IPs, tomamos la primera
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private void checkRateLimit(String ip) {
        RateLimitInfo info = loginAttemptsByIp.get(ip);
        if (info != null && info.intentos >= MAX_ATTEMPTS_PER_IP) {
            if (info.ultimoIntento.plusMinutes(BLOCK_TIME_MINUTES).isAfter(LocalDateTime.now())) {
                throw new LockedException("Demasiados intentos de inicio de sesión desde esta dirección IP. Bloqueada temporalmente.");
            } else {
                loginAttemptsByIp.remove(ip); // Reiniciar tras tiempo de bloqueo
            }
        }
    }

    private void registerFailedAttempt(String ip) {
        loginAttemptsByIp.compute(ip, (key, val) -> {
            if (val == null) return new RateLimitInfo(1, LocalDateTime.now());
            val.intentos++;
            val.ultimoIntento = LocalDateTime.now();
            return val;
        });
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername().contains("|")) {
            throw new IllegalArgumentException("El nombre de usuario no puede contener el carácter '|'");
        }
        Usuario user = new Usuario();
        user.setUsername(request.getUsername());
        if (request.getRol() == Rol.super_admin) {
            throw new IllegalArgumentException("No se puede registrar un usuario con rol de super_admin desde la interfaz.");
        }
        user.setNombreCompleto(request.getNombreCompleto());
        user.setRol(request.getRol());
        user.setActivo(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIntentosFallidos(0);
        user.setCuentaBloqueada(false);

        Usuario guardado = usuarioRepository.save(user);
        String jwtToken = jwtService.generateToken(guardado);

        auditoriaServiceImpl.registrarAccion(
                TipoAccion.CREAR,
                "USUARIO",
                guardado.getUsername(),
                "Nuevo empleado registrado. Nombre: " + guardado.getNombreCompleto() + ". Rol: " + guardado.getRol()
        );

        return AuthResponse.builder()
                .token(jwtToken)
                .rol(guardado.getRol().name())
                .nombre(guardado.getNombreCompleto())
                .build();
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        checkRateLimit(clientIp);


        Usuario user = null;
        
        // 1. Busca al usuario en la clínica solicitada
        if (request.getClinicaId() != null) {
            user = usuarioRepository.findByUsernameAndClinicaIdClinica(request.getUsername(), request.getClinicaId()).orElse(null);
        }
        
        // 2. Si no lo encuentra, hace fallback buscando si es super_admin
        if (user == null) {
            user = usuarioRepository.findByUsernameAndRol(request.getUsername(), Rol.super_admin).orElse(null);
        }

        // 3. Si ninguna de las dos consultas devuelve un usuario
        if (user == null) {
            registerFailedAttempt(clientIp);
            throw new BadCredentialsException("Credenciales incorrectas o el usuario no pertenece a esta clínica");
        }

        // 2. Comprobar si ya está bloqueado
        if (user.isCuentaBloqueada()) {
            throw new LockedException("Cuenta bloqueada por seguridad. Contacte con un administrador.");
        }

        try {
            String principalToAuthenticate = "ID|" + user.getIdUsuario();
            
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            principalToAuthenticate,
                            request.getPassword()
                    )
            );
            
            // Si llega aquí, es exitoso: limpiar intentos de usuario e IP
            if (user.getIntentosFallidos() > 0) {
                user.setIntentosFallidos(0);
            }
            loginAttemptsByIp.remove(clientIp);

            user.setUltimaConexion(LocalDateTime.now());
            usuarioRepository.save(user);

            // 3. Decidir qué clinicaId va al JWT según jerarquía de roles
            java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
            Long clinicaIdParaToken;

            if (user.getRol() == Rol.super_admin) {
                // Super Admin: el request manda. Si no hay, modo sistema (0 = sin filtro tenant).
                clinicaIdParaToken = request.getClinicaId() != null ? request.getClinicaId() : 0L;
            } else {
                // Usuario normal: validar que el clinicaId del request coincida con el de la DB.
                Long clinicaDelUsuario = user.getClinica() != null ? user.getClinica().getIdClinica() : null;
                if (request.getClinicaId() != null && !request.getClinicaId().equals(clinicaDelUsuario)) {
                    throw new BadCredentialsException("Credenciales incorrectas o el usuario no pertenece a esta clínica");
                }
                clinicaIdParaToken = clinicaDelUsuario;
            }

            if (clinicaIdParaToken != null) {
                extraClaims.put("clinicaId", clinicaIdParaToken);
            }
            String jwtToken = jwtService.generateToken(extraClaims, user);

            auditoriaServiceImpl.registrarAccion(
                    TipoAccion.LOGIN,
                    "SESION",
                    user.getUsername(),
                    "Inicio de sesión exitoso.",
                    user.getUsername()
            );
            return AuthResponse.builder()
                    .token(jwtToken)
                    .rol(user.getRol().name())
                    .nombre(user.getNombreCompleto())
                    .build();

        } catch (BadCredentialsException e) {
            registerFailedAttempt(clientIp);
            
            int nuevosIntentos = user.getIntentosFallidos() + 1;
            user.setIntentosFallidos(nuevosIntentos);

            int maxIntentos = 5;
            if (nuevosIntentos >= maxIntentos) {
                user.setCuentaBloqueada(true);
                usuarioRepository.save(user);
                auditoriaServiceImpl.registrarAccion(
                        TipoAccion.BLOQUEADO,
                        "USUARIO",
                        user.getUsername(),
                        "BLOQUEO AUTOMÁTICO DE CUENTA: Superados 5 intentos fallidos.",
                        user.getUsername(),
                        "Cuenta bloqueada por exceso de intentos"
                );
                throw new LockedException("Has superado el límite de 5 intentos. Tu cuenta ha sido BLOQUEADA.");
            }
            usuarioRepository.save(user);
            int restantes = maxIntentos - nuevosIntentos;
            throw new BadCredentialsException("Credenciales incorrectas. Intentos restantes: " + restantes);
        }
    }
}