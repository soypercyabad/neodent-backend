package com.neodent.cita.repository;

import com.neodent.cita.model.EstadoCita;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoCitaRepository
    extends JpaRepository<EstadoCita, Integer> {

    Optional<EstadoCita>
    findByNombreAndActivoTrue(String nombre);
}