package com.neodent.odontograma.repository;

import com.neodent.odontograma.model.Odontograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdontogramaRepository extends JpaRepository<Odontograma, Long> {

    Optional<Odontograma> findFirstByHistoriaClinicaIdOrderByFechaDesc(Long historiaClinicaId);

    List<Odontograma> findByHistoriaClinicaIdOrderByFechaDesc(Long historiaClinicaId);

    Optional<Odontograma> findByCitaId(Long citaId);

    @Query("SELECT o FROM Odontograma o " +
           "JOIN FETCH o.odontologo od " +
           "JOIN FETCH od.personal p " +
           "WHERE o.historiaClinica.id = :historiaId " +
           "ORDER BY o.fecha DESC")
    List<Odontograma> findHistorialConOdontologo(@Param("historiaId") Long historiaId);
}
