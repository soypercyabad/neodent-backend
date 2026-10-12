package com.neodent.usuario.repository;

import com.neodent.usuario.model.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreAndActivoTrue(String nombre);
    
    Optional<Rol> findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
    
    @EntityGraph(attributePaths = {"modulos"})
    List<Rol> findAllByOrderByNombreAsc();
    
    @EntityGraph(attributePaths = {"modulos"})
    List<Rol> findAllByActivoTrueOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = {"modulos"})
    Optional<Rol> findById(Integer id);

    @EntityGraph(attributePaths = {"modulos"})
    @Query(
        value = """
            SELECT r
            FROM Rol r
            WHERE (:activo IS NULL OR r.activo = :activo)
              AND (
                :tipo IS NULL OR :tipo = '' OR :tipo = 'TODOS' OR
                (:tipo = 'SISTEMA' AND UPPER(r.nombre) IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE')) OR
                (:tipo = 'PERSONALIZADO' AND UPPER(r.nombre) NOT IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE'))
              )
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(r.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(r.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """,
        countQuery = """
            SELECT COUNT(r)
            FROM Rol r
            WHERE (:activo IS NULL OR r.activo = :activo)
              AND (
                :tipo IS NULL OR :tipo = '' OR :tipo = 'TODOS' OR
                (:tipo = 'SISTEMA' AND UPPER(r.nombre) IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE')) OR
                (:tipo = 'PERSONALIZADO' AND UPPER(r.nombre) NOT IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE'))
              )
              AND (
                :buscar IS NULL OR :buscar = '' OR
                LOWER(r.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                LOWER(COALESCE(r.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
              )
        """
    )
    Page<Rol> buscarPaginado(
        @Param("buscar") String buscar,
        @Param("tipo") String tipo,
        @Param("activo") Boolean activo,
        Pageable pageable
    );

    @Query("""
        SELECT
            COUNT(r),
            COALESCE(SUM(CASE WHEN r.activo = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.activo = false THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN UPPER(r.nombre) IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE') THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN UPPER(r.nombre) NOT IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO', 'PACIENTE') THEN 1L ELSE 0L END), 0L)
        FROM Rol r
        WHERE (
            :buscar IS NULL OR :buscar = '' OR
            LOWER(r.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
            LOWER(COALESCE(r.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))
        )
    """)
    List<Object[]> contarPorEstados(
        @Param("buscar") String buscar
    );
}