package com.example.quiropracticoapi.config;

import com.example.quiropracticoapi.service.impl.JwtService;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final String path = request.getServletPath();

        // blindaje: Solo permitimos token por URL para la ruta del handshake del WebSocket (Kiosk)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (path.startsWith("/ws-kiosk")) {
                jwt = request.getParameter("token");
                if (jwt == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        } else {
            jwt = authHeader.substring(7);
        }

        try {
            
            username = jwtService.extractUsername(jwt);

            Long clinicaId = jwtService.extractClinicaId(jwt);

            // Inyectar contexto de clínica lo antes posible para auditoría y logs
            if (clinicaId != null && clinicaId > 0) {
                TenantContext.setTenantId(clinicaId);
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("[DEBUG-AUTH] Error crítico en JwtAuthenticationFilter: ", e);
        } finally {
            // No limpiamos aquí todavía porque el Interceptor lo hará al final de la petición MVC.
            // O si queremos ser ultra-seguros, lo limpiamos si no hay cadena de filtros pendiente.
            // Pero según la arquitectura, el TenantInterceptor.afterCompletion es el encargado.
        }
        filterChain.doFilter(request, response);
    }
}
