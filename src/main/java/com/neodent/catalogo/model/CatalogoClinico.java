package com.neodent.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalogo_clinico", uniqueConstraints = {
    @UniqueConstraint(name = "unq_catalogo_tipo_nombre", columnNames = {"tipo", "nombre"})
})
@Getter
@Setter
@NoArgsConstructor
public class CatalogoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_catalogo")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoCatalogoClinico tipo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public CatalogoClinico(TipoCatalogoClinico tipo, String nombre, String descripcion, Boolean activo) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo != null ? activo : true;
        this.fechaCreacion = LocalDateTime.now();
    }
}
