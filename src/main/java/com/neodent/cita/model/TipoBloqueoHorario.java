package com.neodent.cita.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tipo_bloqueo_horario")
@Getter
@Setter
@NoArgsConstructor
public class TipoBloqueoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_bloqueo")
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "requiere_odontologo", nullable = false)
    private Boolean requiereOdontologo = false;

    @Column(name = "requiere_sede", nullable = false)
    private Boolean requiereSede = false;

    @Column(name = "permite_odontologo", nullable = false)
    private Boolean permiteOdontologo = true;

    @Column(name = "permite_sede", nullable = false)
    private Boolean permiteSede = true;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;
}