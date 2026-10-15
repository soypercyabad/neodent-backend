package com.neodent.legal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_legal")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_legal")
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(name = "grupo")
    private Integer grupo = 1;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public DocumentoLegal(String codigo, String descripcion, Integer grupo, Boolean activo) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.grupo = grupo != null ? grupo : 1;
        this.activo = activo != null ? activo : true;
        this.fechaCreacion = LocalDateTime.now();
    }
}
