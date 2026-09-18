package com.neodent.especialidad.repository;

import com.neodent.especialidad.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {

    Optional<Especialidad> findByIdAndActivoTrue(Integer id);
}