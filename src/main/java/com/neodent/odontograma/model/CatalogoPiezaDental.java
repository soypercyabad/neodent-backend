package com.neodent.odontograma.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogo_pieza_dental")
@Getter
@Setter
@NoArgsConstructor
public class CatalogoPiezaDental {

    @Id
    @Column(name = "numero_pieza")
    private Integer numeroPieza;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_denticion", nullable = false, length = 20)
    private String tipoDenticion;

    @Column(name = "cuadrante", nullable = false)
    private Integer cuadrante;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public CatalogoPiezaDental(Integer numeroPieza, String nombre, String tipoDenticion, Integer cuadrante, Boolean activo) {
        this.numeroPieza = numeroPieza;
        this.nombre = nombre;
        this.tipoDenticion = tipoDenticion;
        this.cuadrante = cuadrante;
        this.activo = activo != null ? activo : true;
    }
}
