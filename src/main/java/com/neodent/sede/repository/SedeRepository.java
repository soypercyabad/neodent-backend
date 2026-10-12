package com.neodent.sede.repository;

import com.neodent.sede.model.Sede;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SedeRepository extends JpaRepository<Sede, Integer> {

    Optional<Sede> findByIdAndActivoTrue(Integer id);

    List<Sede> findByActivoTrueOrderByNombreAsc();

    @Query(
        value = """
            SELECT s
            FROM Sede s
            WHERE (:activo IS NULL OR s.activo = :activo)
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(s.direccion) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.distrito, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.provincia, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.departamento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.telefono, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """,
        countQuery = """
            SELECT COUNT(s)
            FROM Sede s
            WHERE (:activo IS NULL OR s.activo = :activo)
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(s.direccion) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.distrito, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.provincia, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.departamento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.telefono, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """
    )
    Page<Sede> buscarPaginado(
        @Param("buscar") String buscar,
        @Param("activo") Boolean activo,
        Pageable pageable
    );

    @Query("""
        SELECT
            COUNT(s),
            COALESCE(SUM(CASE WHEN s.activo = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.activo = false THEN 1L ELSE 0L END), 0L)
        FROM Sede s
        WHERE (
            :buscar IS NULL OR :buscar = '' OR
            LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(s.direccion) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(s.distrito, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(s.provincia, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(s.departamento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(s.telefono, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
        )
    """)
    List<Object[]> contarPorEstados(
        @Param("buscar") String buscar
    );
}