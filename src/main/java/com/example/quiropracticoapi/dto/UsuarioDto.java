package com.example.quiropracticoapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDto {
    private Integer idUsuario;
    private String nombreCompleto;
    private String username;
}