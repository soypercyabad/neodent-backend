package com.neodent.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modulo")
@Getter
@Setter
@NoArgsConstructor
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modulo")
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Column(name = "ruta", length = 80)
    private String ruta;

    @Column(name = "icono", nullable = false, length = 30)
    private String icono;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "id_modulo_padre")
    private Integer idModuloPadre;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Modulo modulo = (Modulo) o;
        return id != null && id.equals(modulo.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
