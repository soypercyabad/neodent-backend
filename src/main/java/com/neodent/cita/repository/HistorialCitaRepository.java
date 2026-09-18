package com.neodent.cita.repository;

import com.neodent.cita.model.HistorialCita;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialCitaRepository
    extends JpaRepository<HistorialCita, Long> {
}