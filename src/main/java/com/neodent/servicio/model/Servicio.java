package com.neodent.servicio.model;

import com.neodent.especialidad.model.Especialidad;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "servicio",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {
            "id_especialidad",
            "nombre"
        }
    )
)
@Getter @Setter
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Short duracionMinutos = 30;

    @Column(
        name = "precio_referencial",
        precision = 10,
        scale = 2
    )
    private BigDecimal precioReferencial;

    @Column(nullable = false)
    private Boolean activo = true;
}