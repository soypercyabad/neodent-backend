package com.neodent.cita.repository;

import com.neodent.cita.model.HorarioOdontologo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
          AND h.horaInicio <= :inicio
          AND h.horaFin >= :fin
    """)
    boolean existeHorarioDisponible(
        Long odontologoEspecialidadId,
        Integer sedeId,
        Byte diaSemana,
        LocalTime inicio,
        LocalTime fin
    );

    List<HorarioOdontologo>
    findByOdontologoEspecialidadIdAndSedeIdAndDiaSemanaAndActivoTrueOrderByHoraInicio(
        Long odontologoEspecialidadId,
        Integer sedeId,
        Byte diaSemana
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
        AND (:horarioId IS NULL OR h.id <> :horarioId)
        AND h.horaInicio < :horaFin
        AND h.horaFin > :horaInicio
    """)
    boolean existeCruceHorario(
        @Param("horarioId") Long horarioId,
        @Param("odontologoId") Long odontologoId,
        @Param("diaSemana") Byte diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin
    );
}