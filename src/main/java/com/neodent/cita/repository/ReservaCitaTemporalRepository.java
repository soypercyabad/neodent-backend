package com.neodent.cita.repository;

import com.neodent.cita.model.ReservaCitaTemporal;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReservaCitaTemporalRepository
    extends JpaRepository<ReservaCitaTemporal, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM ReservaCitaTemporal r
        WHERE r.tokenReserva = :tokenReserva
          AND r.confirmada = false
    """)
    Optional<ReservaCitaTemporal>
    findPorTokenParaConfirmar(
        @Param("tokenReserva")
        String tokenReserva
    );


    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservaCitaTemporal r
        WHERE r.odontologoEspecialidad.odontologo.id = :odontologoId
        AND r.confirmada = false
        AND r.expiresAt > :ahora
        AND r.fechaHoraInicio < :fin
        AND r.fechaHoraFin > :inicio
    """)
    boolean existeReservaVigente(
        @Param("odontologoId")
        Long odontologoId,

        @Param("inicio")
        LocalDateTime inicio,

        @Param("fin")
        LocalDateTime fin,

        @Param("ahora")
        LocalDateTime ahora
    );

    @Query("""
        SELECT r
        FROM ReservaCitaTemporal r
        WHERE r.odontologoEspecialidad.odontologo.id = :odontologoId
        AND r.confirmada = false
        AND r.expiresAt > :ahora
        AND r.fechaHoraInicio < :fin
        AND r.fechaHoraFin > :inicio
    """)
    java.util.List<ReservaCitaTemporal> buscarReservasVigentesDelDia(
        @Param("odontologoId") Long odontologoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("ahora") LocalDateTime ahora
    );
}