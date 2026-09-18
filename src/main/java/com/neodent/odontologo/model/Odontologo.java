package com.neodent.odontologo.model;

import com.neodent.personal.model.Personal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "odontologo")
@Getter
@Setter
@NoArgsConstructor
public class Odontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_odontologo")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_personal",
        nullable = false,
        unique = true
    )
    private Personal personal;

    @Column(
        name = "numero_colegiatura",
        nullable = false,
        unique = true,
        length = 30
    )
    private String numeroColegiatura;

    @Column(
        name = "activo",
        nullable = false
    )
    private Boolean activo = true;

    @Column(
        name = "fecha_creacion",
        insertable = false,
        updatable = false
    )
    private LocalDateTime fechaCreacion;
}