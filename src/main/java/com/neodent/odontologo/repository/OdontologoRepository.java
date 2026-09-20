package com.neodent.odontologo.repository;

import com.neodent.odontologo.model.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    Optional<Odontologo> findByPersonalId(Long personalId);

    List<Odontologo> findAllByPersonalIdIn(List<Long> personalIds);

    Optional<Odontologo> findByIdAndActivoTrue(Long id);

    boolean existsByNumeroColegiaturaIgnoreCase(String numeroColegiatura);

    boolean existsByNumeroColegiaturaIgnoreCaseAndIdNot(String numeroColegiatura, Long id);
}