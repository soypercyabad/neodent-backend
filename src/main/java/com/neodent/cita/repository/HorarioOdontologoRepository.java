package com.neodent.cita.repository;

import com.neodent.cita.model.HorarioOdontologo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
        SELECT DISTINCT h.odontologoEspecialidad.especialidad.id, h.sede.id
        FROM HorarioOdontologo h
        WHERE h.activo = true
          AND h.sede.activo = true
          AND h.odontologoEspecialidad.activo = true
          AND h.odontologoEspecialidad.especialidad.activo = true
          AND h.odontologoEspecialidad.odontologo.activo = true
          AND h.odontologoEspecialidad.odontologo.personal.activo = true
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= CURRENT_DATE)
    """)
    List<Object[]> listarEspecialidadesYSedesConHorariosActivos();

    @Query("""
        SELECT DISTINCT h.odontologoEspecialidad.id
        FROM HorarioOdontologo h
        WHERE h.activo = true
          AND h.sede.activo = true
          AND (:sedeId IS NULL OR h.sede.id = :sedeId)
          AND h.odontologoEspecialidad.especialidad.id = :especialidadId
          AND h.odontologoEspecialidad.activo = true
          AND h.odontologoEspecialidad.especialidad.activo = true
          AND h.odontologoEspecialidad.odontologo.activo = true
          AND h.odontologoEspecialidad.odontologo.personal.activo = true
          AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= CURRENT_DATE)
    """)
    List<Long> listarOdontologoEspecialidadIdsConHorario(
        @Param("especialidadId") Integer especialidadId,
        @Param("sedeId") Integer sedeId
    );

    List<HorarioOdontologo> findByActivoTrueOrderByDiaSemanaAscHoraInicioAsc();

    List<HorarioOdontologo> findByOdontologoEspecialidadIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
        Long odontologoEspecialidadId
    );

    List<HorarioOdontologo> findByOdontologoEspecialidadOdontologoIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
        Long odontologoId
    );

    @Query("""
        SELECT h
        FROM HorarioOdontologo h
        JOIN h.odontologoEspecialidad oe
        JOIN oe.odontologo o
        JOIN o.personal p
        JOIN oe.especialidad e
        JOIN h.sede s
        WHERE h.activo = true
          AND (:odontologoEspecialidadId IS NULL OR oe.id = :odontologoEspecialidadId)
          AND (:odontologoId IS NULL OR o.id = :odontologoId)
          AND (:especialidadId IS NULL OR e.id = :especialidadId)
          AND (:sedeId IS NULL OR s.id = :sedeId)
          AND (:diaSemana IS NULL OR h.diaSemana = :diaSemana)
          AND (
            :estado IS NULL OR :estado = '' OR :estado = 'TODOS' OR
            ((:estado = 'VIGENTE' OR :estado = 'VIGENTES') AND h.fechaInicioVigencia <= CURRENT_DATE AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= CURRENT_DATE)) OR
            ((:estado = 'PROXIMO' OR :estado = 'PROXIMOS') AND h.fechaInicioVigencia > CURRENT_DATE) OR
            ((:estado = 'EXPIRADO' OR :estado = 'EXPIRADOS') AND h.fechaFinVigencia IS NOT NULL AND h.fechaFinVigencia < CURRENT_DATE)
          )
          AND (
            :buscar IS NULL OR :buscar = '' OR
            LOWER(p.nombres) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(p.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(p.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(CONCAT(p.nombres, ' ', p.apellidoPaterno, ' ', COALESCE(p.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(o.numeroColegiatura, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
          )
    """)
    Page<HorarioOdontologo> buscarPaginado(
        @Param("odontologoEspecialidadId") Long odontologoEspecialidadId,
        @Param("odontologoId") Long odontologoId,
        @Param("especialidadId") Integer especialidadId,
        @Param("sedeId") Integer sedeId,
        @Param("diaSemana") Byte diaSemana,
        @Param("estado") String estado,
        @Param("buscar") String buscar,
        Pageable pageable
    );

    @Query("""
        SELECT
            COUNT(h),
            COALESCE(SUM(CASE WHEN h.fechaInicioVigencia <= CURRENT_DATE AND (h.fechaFinVigencia IS NULL OR h.fechaFinVigencia >= CURRENT_DATE) THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN h.fechaInicioVigencia > CURRENT_DATE THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN h.fechaFinVigencia IS NOT NULL AND h.fechaFinVigencia < CURRENT_DATE THEN 1L ELSE 0L END), 0L)
        FROM HorarioOdontologo h
        JOIN h.odontologoEspecialidad oe
        JOIN oe.odontologo o
        JOIN o.personal p
        JOIN oe.especialidad e
        JOIN h.sede s
        WHERE h.activo = true
          AND (:odontologoEspecialidadId IS NULL OR oe.id = :odontologoEspecialidadId)
          AND (:odontologoId IS NULL OR o.id = :odontologoId)
          AND (:especialidadId IS NULL OR e.id = :especialidadId)
          AND (:sedeId IS NULL OR s.id = :sedeId)
          AND (:diaSemana IS NULL OR h.diaSemana = :diaSemana)
          AND (
            :buscar IS NULL OR :buscar = '' OR
            LOWER(p.nombres) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(p.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(p.apellidoMaterno, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(CONCAT(p.nombres, ' ', p.apellidoPaterno, ' ', COALESCE(p.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(o.numeroColegiatura, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
          )
    """)
    List<Object[]> contarPorEstados(
        @Param("odontologoEspecialidadId") Long odontologoEspecialidadId,
        @Param("odontologoId") Long odontologoId,
        @Param("especialidadId") Integer especialidadId,
        @Param("sedeId") Integer sedeId,
        @Param("diaSemana") Byte diaSemana,
        @Param("buscar") String buscar
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
