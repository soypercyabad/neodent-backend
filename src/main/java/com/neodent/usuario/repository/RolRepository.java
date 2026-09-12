package com.neodent.usuario.repository;

import com.neodent.usuario.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository
        extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreAndActivoTrue(
        String nombre
    );
}