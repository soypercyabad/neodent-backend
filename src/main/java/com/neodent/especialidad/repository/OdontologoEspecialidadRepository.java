package com.neodent.especialidad.repository;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OdontologoEspecialidadRepository
    extends JpaRepository<OdontologoEspecialidad, Long> {

    Optional<OdontologoEspecialidad>
    findByIdAndActivoTrue(Long id);
}