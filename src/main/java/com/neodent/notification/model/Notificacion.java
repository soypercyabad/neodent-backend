package com.neodent.notification.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(length = 255)
    private String enlace;

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "leida_en")
    private LocalDateTime leidaEn;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
