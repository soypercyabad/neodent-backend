package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository
    extends JpaRepository<Cita, Long> {

    @Query("""
        SELECT COUNT(c) > 0
        FROM Cita c
        WHERE c.odontologoEspecialidad.odontologo.id = :odontologoId
        AND c.estado.nombre NOT IN ('CANCELADA', 'NO_ASISTIO')
        AND c.fechaHoraInicio < :fin
        AND c.fechaHoraFin > :inicio
    """)
    boolean existeCitaActivaEnHorario(
        Long odontologoId,
        LocalDateTime inicio,
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

    @Query("""
        SELECT c
        FROM Cita c
        JOIN FETCH c.estado
        JOIN FETCH c.paciente
        JOIN FETCH c.odontologoEspecialidad oe
        JOIN FETCH oe.odontologo o
        JOIN FETCH oe.especialidad
        JOIN FETCH c.sede
        WHERE c.paciente.usuario.id = :usuarioId
        ORDER BY c.fechaHoraInicio DESC
    """)
    List<Cita> findByPacienteUsuarioIdOrderByFechaHoraInicioDesc(@Param("usuarioId") Long usuarioId);

    @Query("""
        SELECT c
        FROM Cita c
        JOIN FETCH c.estado
        JOIN FETCH c.paciente
        JOIN FETCH c.odontologoEspecialidad oe
        JOIN FETCH oe.odontologo o
        JOIN FETCH oe.especialidad
        JOIN FETCH c.sede
        WHERE oe.odontologo.personal.usuario.id = :usuarioId
        ORDER BY c.fechaHoraInicio DESC
    """)
    List<Cita> findByOdontologoEspecialidadOdontologoPersonalUsuarioIdOrderByFechaHoraInicioDesc(@Param("usuarioId") Long usuarioId);

    @Query("""
        SELECT c
        FROM Cita c
        JOIN FETCH c.estado
        JOIN FETCH c.paciente
        JOIN FETCH c.odontologoEspecialidad oe
        JOIN FETCH oe.odontologo o
        JOIN FETCH oe.especialidad
        JOIN FETCH c.sede
        WHERE (:odontologoId IS NULL OR oe.odontologo.id = :odontologoId)
        AND (:estado IS NULL OR c.estado.nombre = :estado)
        AND (:sedeId IS NULL OR c.sede.id = :sedeId)
        AND (:inicioDia IS NULL OR c.fechaHoraInicio >= :inicioDia)
        AND (:finDia IS NULL OR c.fechaHoraInicio < :finDia)
        ORDER BY c.fechaHoraInicio DESC
    """)
    List<Cita> buscarAgenda(
        @Param("odontologoId") Long odontologoId,
        @Param("estado") String estado,
        @Param("sedeId") Integer sedeId,
        @Param("inicioDia") LocalDateTime inicioDia,
        @Param("finDia") LocalDateTime finDia
    );
}