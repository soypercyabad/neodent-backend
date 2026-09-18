package com.neodent.cita.repository;

import com.neodent.cita.model.BloqueoHorario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueoHorarioRepository
    extends JpaRepository<BloqueoHorario, Long> {

    @Query(
        value = """
            SELECT COUNT(*)
            FROM bloqueo_horario b
            WHERE b.id_odontologo = :odontologoId
              AND (b.id_sede IS NULL OR b.id_sede = :sedeId)
              AND b.fecha_inicio < :fin
              AND b.fecha_fin > :inicio
            """,
        nativeQuery = true
    )
    long contarBloqueos(
        @Param("odontologoId") Long odontologoId,
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        WHERE b.odontologo.id = :odontologoId
          AND (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    List<BloqueoHorario> buscarBloqueosDelDia(
        @Param("odontologoId") Long odontologoId, 
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    List<BloqueoHorario> findAllByOrderByFechaInicioDesc();

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.odontologo.id = :odontologoId
        AND (:sedeId IS NULL OR b.sede IS NULL OR b.sede.id = :sedeId)
        AND b.fechaInicio < :fin
        AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueo(
        @Param("odontologoId") Long odontologoId,
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
}