package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository
    extends JpaRepository<Cita, Long>, CitaRepositoryCustom {

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.estado.nombre IN (
            'PROGRAMADA',
            'CONFIRMADA',
            'EN_ATENCION'
        )
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeCitaActivaGlobalEnHorario(
        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.estado.nombre IN (
            'PROGRAMADA',
            'CONFIRMADA',
            'EN_ATENCION'
        )
        AND c.sede.id = :sedeId
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeCitaActivaGlobalEnHorarioYSede(
        @Param("sedeId")
        Integer sedeId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.odontologoEspecialidad.odontologo.id = :odontologoId
        AND c.estado.nombre IN (
            'PROGRAMADA',
            'CONFIRMADA',
            'EN_ATENCION'
        )
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeCitaActivaEnHorario(
        @Param("odontologoId")
        Long odontologoId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.odontologoEspecialidad.odontologo.id = :odontologoId
        AND c.estado.nombre IN (
            'PROGRAMADA',
            'CONFIRMADA',
            'EN_ATENCION'
        )
        AND c.sede.id = :sedeId
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeCitaActivaEnHorarioYSede(
        @Param("odontologoId")
        Long odontologoId,

        @Param("sedeId")
        Integer sedeId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Cita c
        WHERE c.id = :citaId
    """)
    Optional<Cita> findByIdParaActualizar(
        @Param("citaId")
        Long citaId
    );


    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.id <> :citaId
        AND c.odontologoEspecialidad.odontologo.id = :odontologoId
        AND c.estado.nombre NOT IN ('CANCELADA', 'NO_ASISTIO')
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeOtraCitaActivaEnHorario(
        @Param("citaId")
        Long citaId,

        @Param("odontologoId")
        Long odontologoId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin
    );

    @Query("""
        SELECT c
        FROM Cita c
        WHERE c.odontologoEspecialidad.odontologo.id = :odontologoId
        AND c.estado.nombre NOT IN ('CANCELADA', 'NO_ASISTIO')
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    List<Cita> buscarCitasActivasDelDia(
        @Param("odontologoId") Long odontologoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query(
        value = """
            SELECT c
            FROM Cita c
            JOIN FETCH c.estado
            JOIN FETCH c.paciente
            JOIN FETCH c.odontologoEspecialidad oe
            JOIN FETCH oe.odontologo o
            JOIN FETCH oe.especialidad
            JOIN FETCH c.sede
            WHERE c.paciente.usuario.id = :usuarioId
        """,
        countQuery = """
            SELECT COUNT(c)
            FROM Cita c
            WHERE c.paciente.usuario.id = :usuarioId
        """
    )
    Page<Cita> findByPacienteUsuarioIdOrderByFechaHoraInicioDesc(
        @Param("usuarioId") Long usuarioId,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT c
            FROM Cita c
            JOIN FETCH c.estado
            JOIN FETCH c.paciente
            JOIN FETCH c.odontologoEspecialidad oe
            JOIN FETCH oe.odontologo o
            JOIN FETCH oe.especialidad
            JOIN FETCH c.sede
            WHERE oe.odontologo.personal.usuario.id = :usuarioId
        """,
        countQuery = """
            SELECT COUNT(c)
            FROM Cita c
            JOIN c.odontologoEspecialidad oe
            JOIN oe.odontologo o
            JOIN o.personal per
            JOIN per.usuario u
            WHERE u.id = :usuarioId
        """
    )
    Page<Cita> findByOdontologoEspecialidadOdontologoPersonalUsuarioIdOrderByFechaHoraInicioDesc(
        @Param("usuarioId") Long usuarioId,
        Pageable pageable
    );

    default List<Cita> buscarAgenda(
        Long odontologoId,
        String estado,
        Integer sedeId,
        LocalDateTime inicioDia,
        LocalDateTime finDia
    ) {
        return buscarAgendaDinamica(odontologoId, estado, sedeId, inicioDia, finDia);
    }
}