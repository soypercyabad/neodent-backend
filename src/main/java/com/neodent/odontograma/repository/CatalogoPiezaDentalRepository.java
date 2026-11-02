package com.neodent.odontograma.repository;

import com.neodent.odontograma.model.CatalogoPiezaDental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoPiezaDentalRepository extends JpaRepository<CatalogoPiezaDental, Integer> {
    List<CatalogoPiezaDental> findByActivoTrueOrderByNumeroPiezaAsc();
}
