package com.neodent.cita.repository;

import com.neodent.cita.model.HorarioOdontologo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface HorarioOdontologoRepository
    extends JpaRepository<HorarioOdontologo, Long> {

    @Query("""
        SELECT COUNT(h) > 0
        FROM HorarioOdontologo h
        WHERE h.odontologoEspecialidad.id = :odontologoEspecialidadId
          AND h.sede.id = :sedeId
          AND h.diaSemana = :diaSemana
          AND h.activo = true
          AND h.fechaInicioVigencia <= :fecha
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= :fecha)
          AND h.horaInicio <= :inicio
          AND h.horaFin >= :fin
    """)
    boolean existeHorarioDisponible(
        @Param("odontologoEspecialidadId") Long odontologoEspecialidadId,
        @Param("sedeId") Integer sedeId,
        @Param("diaSemana") Byte diaSemana,
        @Param("fecha") LocalDate fecha,
        @Param("inicio") LocalTime inicio,
        @Param("fin") LocalTime fin
    );

    @Query("""
        SELECT h
        FROM HorarioOdontologo h
        WHERE h.odontologoEspecialidad.id = :odontologoEspecialidadId
          AND h.sede.id = :sedeId
          AND h.diaSemana = :diaSemana
          AND h.activo = true
          AND h.fechaInicioVigencia <= :fecha
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= :fecha)
        ORDER BY h.horaInicio ASC
    """)
    List<HorarioOdontologo> buscarHorariosVigentesParaFecha(
        @Param("odontologoEspecialidadId") Long odontologoEspecialidadId,
        @Param("sedeId") Integer sedeId,
        @Param("diaSemana") Byte diaSemana,
        @Param("fecha") LocalDate fecha
    );

    List<HorarioOdontologo> findByActivoTrueOrderByDiaSemanaAscHoraInicioAsc();

    List<HorarioOdontologo> findByOdontologoEspecialidadIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
        Long odontologoEspecialidadId
    );

    @Query("""
        SELECT COUNT(h) > 0
        FROM HorarioOdontologo h
        WHERE h.odontologoEspecialidad.odontologo.id = :odontologoId
          AND h.diaSemana = :diaSemana
          AND h.activo = true
          AND h.horaInicio < :horaFin
          AND h.horaFin > :horaInicio
          AND h.fechaInicioVigencia <= :fechaFinComparacion
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= :fechaInicioVigencia)
    """)
    boolean existeCruceHorarioNuevo(
        @Param("odontologoId") Long odontologoId,
        @Param("diaSemana") Byte diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin,
        @Param("fechaInicioVigencia") LocalDate fechaInicioVigencia,
        @Param("fechaFinComparacion") LocalDate fechaFinComparacion
    );

    @Query("""
        SELECT COUNT(h) > 0
        FROM HorarioOdontologo h
        WHERE h.odontologoEspecialidad.odontologo.id = :odontologoId
          AND h.diaSemana = :diaSemana
          AND h.activo = true
          AND h.id <> :horarioId
          AND h.horaInicio < :horaFin
          AND h.horaFin > :horaInicio
          AND h.fechaInicioVigencia <= :fechaFinComparacion
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= :fechaInicioVigencia)
    """)
    boolean existeCruceHorarioOtro(
        @Param("horarioId") Long horarioId,
        @Param("odontologoId") Long odontologoId,
        @Param("diaSemana") Byte diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin,
        @Param("fechaInicioVigencia") LocalDate fechaInicioVigencia,
        @Param("fechaFinComparacion") LocalDate fechaFinComparacion
    );

    default boolean existeCruceHorario(
        Long horarioId,
        Long odontologoId,
        Byte diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinComparacion
    ) {
        if (horarioId == null) {
            return existeCruceHorarioNuevo(
                odontologoId,
                diaSemana,
                horaInicio,
                horaFin,
                fechaInicioVigencia,
                fechaFinComparacion
            );
        }
        return existeCruceHorarioOtro(
            horarioId,
            odontologoId,
            diaSemana,
            horaInicio,
            horaFin,
            fechaInicioVigencia,
            fechaFinComparacion
        );
    }
}
