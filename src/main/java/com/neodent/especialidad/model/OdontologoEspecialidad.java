package com.neodent.especialidad.model;

import com.neodent.odontologo.model.Odontologo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "odontologo_especialidad",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {
            "id_odontologo",
            "id_especialidad"
        }
    )
)
@Getter @Setter
@NoArgsConstructor
public class OdontologoEspecialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_odontologo_especialidad")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_odontologo", nullable = false)
    private Odontologo odontologo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false)
    private Boolean activo = true;
}