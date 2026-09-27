package com.neodent.servicio.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "servicio_sede",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_servicio", "id_sede"}))
@Getter @Setter
@NoArgsConstructor
public class ServicioSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_sede")
    private Long id;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_sede", nullable = false)
    private Integer idSede;

    @Column(nullable = false)
    private Boolean activo = true;
}