package com.neodent.usuario.repository;

import com.neodent.usuario.model.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoUsuarioRepository
        extends JpaRepository<EstadoUsuario, Integer> {

    Optional<EstadoUsuario> findByNombreAndActivoTrue(
        String nombre
    );
}