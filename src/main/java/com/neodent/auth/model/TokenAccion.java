package com.neodent.auth.model;

import com.neodent.paciente.model.Paciente;
import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_accion")
@Getter
@Setter
@NoArgsConstructor
public class TokenAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token_accion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean usado = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private Boolean revocado = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}