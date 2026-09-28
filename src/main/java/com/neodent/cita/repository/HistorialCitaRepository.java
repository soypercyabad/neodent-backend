package com.neodent.cita.repository;

import com.neodent.cita.model.HistorialCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialCitaRepository extends JpaRepository<HistorialCita, Long> {
    List<HistorialCita> findByCitaIdOrderByFechaCreacionDesc(Long citaId);
}