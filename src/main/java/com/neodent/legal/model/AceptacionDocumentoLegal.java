package com.neodent.legal.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "aceptacion_documento_legal", uniqueConstraints = {
    @UniqueConstraint(name = "unq_usuario_doc_version", columnNames = {"id_usuario", "id_documento_legal_version"})
})
@Getter
@Setter
@NoArgsConstructor
public class AceptacionDocumentoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aceptacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_legal_version", nullable = false)
    private DocumentoLegalVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean aceptado = true;

    @Column(nullable = false, length = 30)
    private String origen = "Web";

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "agente_usuario", length = 500)
    private String agenteUsuario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    public AceptacionDocumentoLegal(DocumentoLegalVersion version, Usuario usuario,
                                   Boolean aceptado, String origen, String direccionIp,
                                   String agenteUsuario) {
        this.version = version;
        this.usuario = usuario;
        this.aceptado = aceptado != null ? aceptado : true;
        this.origen = (origen != null && !origen.isBlank()) ? origen : "Web";
        this.direccionIp = direccionIp;
        this.agenteUsuario = agenteUsuario;
        this.fechaCreacion = LocalDateTime.now();
    }
}
