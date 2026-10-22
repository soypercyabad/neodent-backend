package com.neodent.odontograma.repository;

import com.neodent.odontograma.model.CondicionOdontograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CondicionOdontogramaRepository extends JpaRepository<CondicionOdontograma, Integer> {

    List<CondicionOdontograma> findByActivoTrueOrderByIdAsc();

    Optional<CondicionOdontograma> findByCodigoIgnoreCase(String codigo);
}
