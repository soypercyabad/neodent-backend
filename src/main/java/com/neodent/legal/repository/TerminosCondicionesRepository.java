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

    List<TerminosCondiciones> findByVigenteTrueOrderByFechaPublicacionDesc();

    List<TerminosCondiciones> findAllByOrderByFechaCreacionDesc();

    @Modifying
    @Transactional
    @Query("UPDATE TerminosCondiciones t SET t.vigente = false")
    void desactivarTodos();

    boolean existsByVersion(String version);

    @Query(value = """
        SELECT t FROM TerminosCondiciones t
        WHERE (:buscar IS NULL OR :buscar = '' OR
               LOWER(t.titulo) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
               LOWER(t.version) LIKE LOWER(CONCAT('%', :buscar, '%')))
          AND (:activo IS NULL OR t.vigente = :activo)
    """,
    countQuery = """
        SELECT COUNT(t) FROM TerminosCondiciones t
        WHERE (:buscar IS NULL OR :buscar = '' OR
               LOWER(t.titulo) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
               LOWER(t.version) LIKE LOWER(CONCAT('%', :buscar, '%')))
          AND (:activo IS NULL OR t.vigente = :activo)
    """)
    org.springframework.data.domain.Page<TerminosCondiciones> buscarPaginado(
        @org.springframework.data.repository.query.Param("buscar") String buscar,
        @org.springframework.data.repository.query.Param("activo") Boolean activo,
        org.springframework.data.domain.Pageable pageable
    );
}
