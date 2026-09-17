package com.neodent.cita.model;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.paciente.model.Paciente;
import com.neodent.sede.model.Sede;
import com.neodent.servicio.model.Servicio;
import com.neodent.usuario.model.Usuario;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_odontologo_especialidad",
        nullable = false
    )
    private OdontologoEspecialidad odontologoEspecialidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado_cita", nullable = false)
    private EstadoCita estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio_solicitado")
    private Servicio servicioSolicitado;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(length = 255)
    private String motivo;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "confirmada_en")
    private LocalDateTime confirmadaEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Usuario creadoPor;

    @Column(
        name = "created_at",
        insertable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
        name = "updated_at",
        insertable = false,
        updatable = false
    )
    private LocalDateTime updatedAt;
}