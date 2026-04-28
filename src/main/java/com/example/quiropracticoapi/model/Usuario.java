package com.example.quiropracticoapi.model;

import com.example.quiropracticoapi.model.enums.Rol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username", "clinica_id"})
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "clinica_id = :clinicaId")
public class Usuario extends BaseAuditEntity implements UserDetails, SoftDeletable, Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private  String password;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Column(name = "activo", nullable = false)
    private boolean activo= true;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos = 0;

    @Column(name = "cuenta_bloqueada")
    private boolean cuentaBloqueada = false;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    /** Ruta en R2 de la foto de perfil. Nullable si no tiene foto. 
     *  Ej: "usuarios/5/perfil/foto_perfil.jpg" */
    @Column(name = "foto_perfil_path", length = 500)
    private String fotoPerfilPath;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+ rol.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.cuentaBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.activo;
    }

    @Override
    public boolean isEliminadoLogico() {
        return !this.activo;
    }

    @Override
    public String toResumen(com.example.quiropracticoapi.model.enums.TipoAccion accion) {
        return String.format("Usuario #%s | %s (%s) | Activo: %s",
                idUsuario, username, rol, activo ? "Sí" : "No");
    }
}
