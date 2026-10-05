package com.neodent.legal.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "aceptacion_terminos", uniqueConstraints = {
    @UniqueConstraint(name = "unq_usuario_terminos", columnNames = {"id_usuario", "id_terminos"})
})
@Getter
@Setter
@NoArgsConstructor
public class AceptacionTerminos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aceptacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_terminos", nullable = false)
    private TerminosCondiciones terminos;

    @Column(name = "aceptado_en", nullable = false)
    private LocalDateTime aceptadoEn = LocalDateTime.now();

    @Column(length = 45)
    private String ip;

    @Column(name = "agente_usuario", length = 255)
    private String agenteUsuario;
}
