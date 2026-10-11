package com.neodent.sede.repository;

import com.neodent.sede.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SedeRepository extends JpaRepository<Sede, Integer> {

    Optional<Sede> findByIdAndActivoTrue(Integer id);

    List<Sede> findByActivoTrueOrderByNombreAsc();
}