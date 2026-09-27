package com.neodent.odontologo.repository;

import com.neodent.odontologo.model.Odontologo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    Optional<Odontologo> findByPersonalId(Long personalId);

    List<Odontologo> findAllByPersonalIdIn(List<Long> personalIds);

    Optional<Odontologo> findByIdAndActivoTrue(Long id);

    boolean existsByNumeroColegiaturaIgnoreCase(String numeroColegiatura);

    boolean existsByNumeroColegiaturaIgnoreCaseAndIdNot(
        String numeroColegiatura,
        Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Odontologo o WHERE o.id = :id")
    Optional<Odontologo> bloquearParaReserva(@Param("id") Long id);
}