package com.neodent.historia.repository;

import com.neodent.historia.model.EvolucionClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvolucionClinicaRepository extends JpaRepository<EvolucionClinica, Long> {

    @Query("SELECT e FROM EvolucionClinica e " +
           "JOIN FETCH e.odontologo od " +
           "JOIN FETCH od.personal p " +
           "LEFT JOIN FETCH e.cita c " +
           "WHERE e.historiaClinica.id = :historiaId " +
           "ORDER BY e.fecha DESC")
    List<EvolucionClinica> findByHistoriaClinicaIdWithOdontologo(@Param("historiaId") Long historiaId);

    Optional<EvolucionClinica> findByCitaId(Long citaId);
}
