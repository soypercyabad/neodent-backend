package com.neodent.cita.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_cita")
@Getter
@Setter
@NoArgsConstructor
public class HistorialCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_cita")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id")
    private EstadoCita estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_nuevo_id")
    private EstadoCita estadoNuevo;

    @Column(name = "fecha_hora_anterior")
    private LocalDateTime fechaHoraAnterior;

    @Column(name = "fecha_hora_nueva")
    private LocalDateTime fechaHoraNueva;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(
        name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private LocalDateTime createdAt;
}