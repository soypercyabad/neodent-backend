package com.neodent.odontograma.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "condicion_odontograma")
@Getter
@Setter
@NoArgsConstructor
public class CondicionOdontograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condicion")
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "color_hexadecimal", length = 7)
    private String colorHexadecimal;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public CondicionOdontograma(String codigo, String nombre, String descripcion, String colorHexadecimal, Boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.colorHexadecimal = colorHexadecimal;
        this.activo = activo != null ? activo : true;
    }
}
