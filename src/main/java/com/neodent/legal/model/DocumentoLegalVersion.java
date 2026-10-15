package com.neodent.legal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_legal_version", uniqueConstraints = {
    @UniqueConstraint(name = "unq_documento_version", columnNames = {"id_documento_legal", "version"})
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentoLegalVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_legal_version")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_legal", nullable = false)
    private DocumentoLegal documentoLegal;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(name = "url_documento", nullable = false, length = 500)
    private String urlDocumento;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "link_texto", nullable = false, length = 150)
    private String linkTexto;

    @Column(name = "texto_agrupado", length = 255)
    private String textoAgrupado;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public DocumentoLegalVersion(DocumentoLegal documentoLegal, String version, String urlDocumento,
                                 String nombreArchivo, String texto, String linkTexto,
                                 String textoAgrupado, Boolean activo) {
        this.documentoLegal = documentoLegal;
        this.version = version;
        this.urlDocumento = urlDocumento;
        this.nombreArchivo = nombreArchivo;
        this.texto = texto;
        this.linkTexto = linkTexto;
        this.textoAgrupado = textoAgrupado;
        this.activo = activo != null ? activo : true;
        this.fechaCreacion = LocalDateTime.now();
    }
}
