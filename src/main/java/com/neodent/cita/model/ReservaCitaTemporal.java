package com.neodent.cita.model;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.paciente.model.Paciente;
import com.neodent.sede.model.Sede;
import com.neodent.servicio.model.Servicio;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva_cita_temporal")
@Getter
@Setter
@NoArgsConstructor
public class ReservaCitaTemporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    @Column(
        name = "token_reserva",
        nullable = false,
        unique = true,
        length = 100
    )
    private String tokenReserva;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio_solicitado")
    private Servicio servicioSolicitado;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean confirmada = false;

    @Column(
        name = "created_at",
        insertable = false,
        updatable = false
    )
    private LocalDateTime createdAt;
}