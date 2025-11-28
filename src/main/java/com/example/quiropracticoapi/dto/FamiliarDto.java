package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FamiliarDto {
    private Integer idGrupo;
    private Integer idFamiliar;
    private String nombreCompleto;
    private String relacion;
}
