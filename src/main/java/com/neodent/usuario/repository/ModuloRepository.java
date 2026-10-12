package com.neodent.usuario.repository;

import com.neodent.usuario.model.Modulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Integer> {

    List<Modulo> findAllByOrderByOrdenAsc();

    List<Modulo> findAllByActivoTrueOrderByOrdenAsc();

    List<Modulo> findAllByIdInAndActivoTrue(Collection<Integer> ids);

    Optional<Modulo> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

    boolean existsByRutaIgnoreCase(String ruta);

    boolean existsByRutaIgnoreCaseAndIdNot(String ruta, Integer id);

    @Query(
        value = """
            SELECT m
            FROM Modulo m
            WHERE (:activo IS NULL OR m.activo = :activo)
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(m.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(m.codigo) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(m.ruta) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(m.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """,
        countQuery = """
            SELECT COUNT(m)
            FROM Modulo m
            WHERE (:activo IS NULL OR m.activo = :activo)
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(m.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(m.codigo) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(m.ruta) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(m.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """
    )
    Page<Modulo> buscarPaginado(
        @Param("buscar") String buscar,
        @Param("activo") Boolean activo,
        Pageable pageable
    );

    @Query("""
        SELECT
            COUNT(m),
            COALESCE(SUM(CASE WHEN m.activo = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN m.activo = false THEN 1L ELSE 0L END), 0L)
        FROM Modulo m
        WHERE (
            :buscar IS NULL OR :buscar = '' OR
            LOWER(m.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(m.codigo) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(m.ruta) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(m.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
        )
    """)
    List<Object[]> contarPorEstados(
        @Param("buscar") String buscar
    );
}
