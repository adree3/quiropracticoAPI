package com.example.quiropracticoapi.service.impl;

import com.example.quiropracticoapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        // La autenticación es GLOBAL. Usamos findByIdGlobal (Native Query) 
        // para saltarnos el filtro de Hibernate sin manipular la sesión.
        if (usernameOrId.startsWith("ID|")) {
            Integer id = Integer.parseInt(usernameOrId.substring(3));
            return usuarioRepository.findByIdGlobal(id)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado por ID: " + id));
        }
        
        // Si no viene con prefijo ID| (ej. procesos internos o fallos de lógica)
        // No permitimos búsqueda por username global porque no es unívoco
        throw new UsernameNotFoundException("La autenticación por nombre de usuario global no está permitida. Use el formato ID|{id}");
    }
}
