package com.neodent.personal.repository;

import com.neodent.personal.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalRepository
    extends JpaRepository<Personal, Long> {

    Optional<Personal> findByUsuarioIdAndActivoTrue(Long usuarioId);
}