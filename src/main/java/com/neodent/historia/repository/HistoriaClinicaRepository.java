package com.neodent.historia.repository;

import com.neodent.historia.model.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByPacienteId(Long pacienteId);

    boolean existsByPacienteId(Long pacienteId);

    @Query("SELECT h FROM HistoriaClinica h " +
           "JOIN FETCH h.paciente p " +
           "LEFT JOIN FETCH p.tipoDocumento " +
           "LEFT JOIN FETCH p.sedeRegistro " +
           "WHERE p.id = :pacienteId")
    Optional<HistoriaClinica> findByPacienteIdWithDetails(@Param("pacienteId") Long pacienteId);
}
