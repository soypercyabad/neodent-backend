package com.neodent.audit.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 60)
    private String entidad;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "metadatos", columnDefinition = "JSON")
    private String metadatos;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Auditoria(Usuario usuario, String accion, String entidad, Long idEntidad, String descripcion, String ip, String metadatos) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.idEntidad = idEntidad;
        this.descripcion = descripcion;
        this.ip = ip;
        this.metadatos = metadatos;
        this.fechaCreacion = LocalDateTime.now();
    }
}
