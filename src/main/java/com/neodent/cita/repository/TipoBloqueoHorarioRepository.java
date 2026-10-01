package com.neodent.cita.repository;

import com.neodent.cita.model.TipoBloqueoHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoBloqueoHorarioRepository
    extends JpaRepository<TipoBloqueoHorario, Integer> {

    List<TipoBloqueoHorario> findByActivoTrueOrderByNombreAsc();

    List<TipoBloqueoHorario> findAllByOrderByNombreAsc();

    Optional<TipoBloqueoHorario> findByIdAndActivoTrue(Integer id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}