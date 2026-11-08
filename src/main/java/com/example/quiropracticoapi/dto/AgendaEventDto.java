package com.example.quiropracticoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendaEventDto {

    private String action;
    private CitaDto data;
    private Integer idCita;

    public AgendaEventDto(String action, CitaDto data) {
        this.action = action;
        this.data = data;
        this.idCita = (data != null) ? data.getIdCita() : null;
    }

    public AgendaEventDto(String action, Integer idCita) {
        this.action = action;
        this.idCita = idCita;
    }
}

