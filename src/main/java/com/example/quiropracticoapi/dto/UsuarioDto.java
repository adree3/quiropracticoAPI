package com.example.quiropracticoapi.dto;

import com.example.quiropracticoapi.model.enums.Rol;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDto {
    private Integer idUsuario;
    private String nombreCompleto;
    private String username;
    private Rol rol;
    private boolean activo;
    private boolean cuentaBloqueada;
}