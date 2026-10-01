package com.neodent.cita.model;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.sede.model.Sede;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "horario_odontologo")
@Getter @Setter
@NoArgsConstructor
public class HorarioOdontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_odontologo_especialidad",
        nullable = false
    )
    private OdontologoEspecialidad odontologoEspecialidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Column(name = "dia_semana", nullable = false)
    private Byte diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_inicio_vigencia", nullable = false)
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @Column(nullable = false)
    private Boolean activo = true;
}
