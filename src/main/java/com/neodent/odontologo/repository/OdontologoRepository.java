package com.neodent.odontologo.repository;

import com.neodent.odontologo.model.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    Optional<Odontologo> findByPersonalId(Long personalId);

    Optional<Odontologo> findByIdAndActivoTrue(Long id);

    boolean existsByNumeroColegiaturaIgnoreCase(String numeroColegiatura);

    boolean existsByNumeroColegiaturaIgnoreCaseAndIdNot(String numeroColegiatura, Long id);
}