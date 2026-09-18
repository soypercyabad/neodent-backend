package com.neodent.auth.model;

import com.neodent.paciente.model.Paciente;
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

    @Column(name = "correo_destino", nullable = false)
    private String correoDestino;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "hash_codigo", nullable = false)
    private String hashCodigo;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private Short intentos = 0;

    @Column(name = "max_intentos", nullable = false)
    private Short maxIntentos = 5;

    @Column(nullable = false)
    private Boolean usado = false;

    @Column(name = "num_reenvios", nullable = false)
    private Short numReenvios = 0;

    @Column(name = "fecha_ultimo_reenvio")
    private LocalDateTime fechaUltimoReenvio;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;
}