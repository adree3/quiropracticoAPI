package com.example.quiropracticoapi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @com.fasterxml.jackson.annotation.JsonAlias({"idClinica", "clinica_id"})
    private Long clinicaId;
}
