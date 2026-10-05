package com.neodent.legal.repository;

import com.neodent.legal.model.TerminosCondiciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminosCondicionesRepository extends JpaRepository<TerminosCondiciones, Long> {

    Optional<TerminosCondiciones> findFirstByVigenteTrueOrderByFechaPublicacionDesc();

    List<TerminosCondiciones> findAllByOrderByFechaCreacionDesc();

    @Modifying
    @Transactional
    @Query("UPDATE TerminosCondiciones t SET t.vigente = false")
    void desactivarTodos();

    boolean existsByVersion(String version);
}
