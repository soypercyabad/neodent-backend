package com.neodent.auth.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_verificacion")
@Getter @Setter
@NoArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "email_destino", nullable = false)
    private String emailDestino;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Short intentos = 0;

    @Column(name = "max_intentos", nullable = false)
    private Short maxIntentos = 5;

    @Column(nullable = false)
    private Boolean usado = false;

    @Column(name = "resend_count", nullable = false)
    private Short resendCount = 0;

    @Column(name = "last_resend_at")
    private LocalDateTime lastResendAt;
}