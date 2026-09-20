package com.neodent.especialidad.repository;

import com.neodent.especialidad.model.OdontologoEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OdontologoEspecialidadRepository extends JpaRepository<OdontologoEspecialidad, Long> {

    Optional<OdontologoEspecialidad> findByIdAndActivoTrue(Long id);

    List<OdontologoEspecialidad> findAllByOdontologoId(Long odontologoId);

    @Query("SELECT oe FROM OdontologoEspecialidad oe JOIN FETCH oe.especialidad e WHERE oe.odontologo.id IN :odontologoIds AND oe.activo = true")
    List<OdontologoEspecialidad> findAllByOdontologoIdInAndActivoTrueWithEspecialidad(@Param("odontologoIds") List<Long> odontologoIds);

    @Query("SELECT oe FROM OdontologoEspecialidad oe JOIN FETCH oe.especialidad e WHERE oe.odontologo.id = :odontologoId AND oe.activo = true")
    List<OdontologoEspecialidad> findAllByOdontologoIdAndActivoTrueWithEspecialidad(@Param("odontologoId") Long odontologoId);

    Optional<OdontologoEspecialidad> findByOdontologoIdAndEspecialidadId(Long odontologoId, Integer especialidadId);
}