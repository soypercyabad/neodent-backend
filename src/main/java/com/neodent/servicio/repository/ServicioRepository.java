package com.neodent.servicio.repository;

import com.neodent.servicio.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicioRepository
    extends JpaRepository<Servicio, Integer> {

    Optional<Servicio> findByIdAndActivoTrue(Integer id);
}