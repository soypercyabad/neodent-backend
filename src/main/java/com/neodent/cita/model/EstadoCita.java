package com.neodent.cita.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estado_cita")
@Getter
@Setter
@NoArgsConstructor
public class EstadoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_cita")
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;
}