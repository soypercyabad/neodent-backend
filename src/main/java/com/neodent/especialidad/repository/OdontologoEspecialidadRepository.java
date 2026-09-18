package com.neodent.especialidad.repository;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontologoEspecialidadRepository extends JpaRepository<OdontologoEspecialidad, Long> {

    Optional<OdontologoEspecialidad> findByIdAndActivoTrue(Long id);

    List<OdontologoEspecialidad> findAllByOdontologoId(Long odontologoId);

    Optional<OdontologoEspecialidad> findByOdontologoIdAndEspecialidadId(Long odontologoId, Integer especialidadId);
}