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
            WHERE
              (b.id_odontologo IS NULL OR b.id_odontologo = :odontologoId)
              AND
              (b.id_sede IS NULL OR b.id_sede = :sedeId)
              AND b.fecha_inicio < :fin
              AND b.fecha_fin > :inicio
            """,
        nativeQuery = true
    )
    long contarBloqueos(
        @Param("odontologoId")
        Long odontologoId,

        @Param("sedeId")
        Integer sedeId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        JOIN FETCH b.tipoBloqueo
        LEFT JOIN FETCH b.odontologo
        LEFT JOIN FETCH b.sede
        WHERE
          (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND
          (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
        ORDER BY b.fechaInicio ASC
    """)
    List<BloqueoHorario> buscarBloqueosDelDia(
        @Param("odontologoId")
        Long odontologoId,

        @Param("sedeId")
        Integer sedeId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoGlobal(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoOdontologo(
        @Param("odontologoId") Long odontologoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoSede(
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoOdontologoYSede(
        @Param("odontologoId") Long odontologoId,
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    default boolean existeCruceBloqueo(
        Long odontologoId,
        Integer sedeId,
        LocalDateTime inicio,
        LocalDateTime fin
    ) {
        if (odontologoId == null && sedeId == null) {
            return existeCruceBloqueoGlobal(inicio, fin);
        } else if (odontologoId != null && sedeId == null) {
            return existeCruceBloqueoOdontologo(odontologoId, inicio, fin);
        } else if (odontologoId == null) {
            return existeCruceBloqueoSede(sedeId, inicio, fin);
        }
        return existeCruceBloqueoOdontologoYSede(odontologoId, sedeId, inicio, fin);
    }

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.id != :idBloqueo
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoGlobalExcluyendo(
        @Param("idBloqueo") Long idBloqueo,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.id != :idBloqueo
          AND (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoOdontologoExcluyendo(
        @Param("idBloqueo") Long idBloqueo,
        @Param("odontologoId") Long odontologoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.id != :idBloqueo
          AND (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoSedeExcluyendo(
        @Param("idBloqueo") Long idBloqueo,
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(b) > 0
        FROM BloqueoHorario b
        WHERE b.id != :idBloqueo
          AND (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND (b.sede IS NULL OR b.sede.id = :sedeId)
          AND b.fechaInicio < :fin
          AND b.fechaFin > :inicio
    """)
    boolean existeCruceBloqueoOdontologoYSedeExcluyendo(
        @Param("idBloqueo") Long idBloqueo,
        @Param("odontologoId") Long odontologoId,
        @Param("sedeId") Integer sedeId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    default boolean existeCruceBloqueoExcluyendo(
        Long idBloqueo,
        Long odontologoId,
        Integer sedeId,
        LocalDateTime inicio,
        LocalDateTime fin
    ) {
        if (odontologoId == null && sedeId == null) {
            return existeCruceBloqueoGlobalExcluyendo(idBloqueo, inicio, fin);
        } else if (odontologoId != null && sedeId == null) {
            return existeCruceBloqueoOdontologoExcluyendo(idBloqueo, odontologoId, inicio, fin);
        } else if (odontologoId == null) {
            return existeCruceBloqueoSedeExcluyendo(idBloqueo, sedeId, inicio, fin);
        }
        return existeCruceBloqueoOdontologoYSedeExcluyendo(idBloqueo, odontologoId, sedeId, inicio, fin);
    }

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        JOIN FETCH b.tipoBloqueo
        LEFT JOIN FETCH b.odontologo
        LEFT JOIN FETCH b.sede
        JOIN FETCH b.creadoPor
        ORDER BY b.fechaInicio DESC
    """)
    List<BloqueoHorario> listarTodos();

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        JOIN FETCH b.tipoBloqueo
        LEFT JOIN FETCH b.odontologo
        LEFT JOIN FETCH b.sede
        JOIN FETCH b.creadoPor
        WHERE (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
        ORDER BY b.fechaInicio DESC
    """)
    List<BloqueoHorario> listarPorOdontologo(@Param("odontologoId") Long odontologoId);

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        JOIN FETCH b.tipoBloqueo
        LEFT JOIN FETCH b.odontologo
        LEFT JOIN FETCH b.sede
        JOIN FETCH b.creadoPor
        WHERE (b.sede IS NULL OR b.sede.id = :sedeId)
        ORDER BY b.fechaInicio DESC
    """)
    List<BloqueoHorario> listarPorSede(@Param("sedeId") Integer sedeId);

    @Query("""
        SELECT b
        FROM BloqueoHorario b
        JOIN FETCH b.tipoBloqueo
        LEFT JOIN FETCH b.odontologo
        LEFT JOIN FETCH b.sede
        JOIN FETCH b.creadoPor
        WHERE (b.odontologo IS NULL OR b.odontologo.id = :odontologoId)
          AND (b.sede IS NULL OR b.sede.id = :sedeId)
        ORDER BY b.fechaInicio DESC
    """)
    List<BloqueoHorario> listarPorOdontologoYSede(
        @Param("odontologoId") Long odontologoId,
        @Param("sedeId") Integer sedeId
    );

    default List<BloqueoHorario> listarFiltrado(
        Long odontologoId,
        Integer sedeId
    ) {
        if (odontologoId == null && sedeId == null) {
            return listarTodos();
        } else if (odontologoId != null && sedeId == null) {
            return listarPorOdontologo(odontologoId);
        } else if (odontologoId == null) {
            return listarPorSede(sedeId);
        }
        return listarPorOdontologoYSede(odontologoId, sedeId);
    }
}