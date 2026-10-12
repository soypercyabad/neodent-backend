package com.neodent.servicio.repository;

import com.neodent.servicio.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    Optional<Servicio> findByIdAndActivoTrue(Integer id);

    boolean existsByEspecialidadIdAndActivoTrue(Integer especialidadId);

    boolean existsByEspecialidadIdAndNombreIgnoreCase(Integer especialidadId, String nombre);

    boolean existsByEspecialidadIdAndNombreIgnoreCaseAndIdNot(Integer especialidadId, String nombre, Integer id);

    @Query(
        value = """
            SELECT s FROM Servicio s
            LEFT JOIN s.especialidad e
            WHERE (:buscar IS NULL OR :buscar = '' OR
                   LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(s.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')))
              AND (:especialidadId IS NULL OR e.id = :especialidadId)
              AND (:sedeId IS NULL OR EXISTS (
                    SELECT 1 FROM ServicioSede ss
                    WHERE ss.idServicio = s.id
                      AND ss.idSede = :sedeId
                      AND ss.activo = true
                  ))
              AND (:activo IS NULL OR s.activo = :activo)
              AND (:destacado IS NULL OR s.destacado = :destacado)
        """,
        countQuery = """
            SELECT COUNT(s) FROM Servicio s
            LEFT JOIN s.especialidad e
            WHERE (:buscar IS NULL OR :buscar = '' OR
                   LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(s.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')))
              AND (:especialidadId IS NULL OR e.id = :especialidadId)
              AND (:sedeId IS NULL OR EXISTS (
                    SELECT 1 FROM ServicioSede ss
                    WHERE ss.idServicio = s.id
                      AND ss.idSede = :sedeId
                      AND ss.activo = true
                  ))
              AND (:activo IS NULL OR s.activo = :activo)
              AND (:destacado IS NULL OR s.destacado = :destacado)
        """
    )
    Page<Servicio> buscarPaginado(
        @Param("buscar") String buscar,
        @Param("especialidadId") Integer especialidadId,
        @Param("sedeId") Integer sedeId,
        @Param("activo") Boolean activo,
        @Param("destacado") Boolean destacado,
        Pageable pageable
    );

    @Query("""
        SELECT
            COUNT(s),
            COALESCE(SUM(CASE WHEN s.destacado = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.activo = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.activo = false THEN 1L ELSE 0L END), 0L)
        FROM Servicio s
        LEFT JOIN s.especialidad e
        WHERE (:buscar IS NULL OR :buscar = '' OR
               LOWER(s.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
               LOWER(COALESCE(s.descripcion, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
               LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')))
          AND (:especialidadId IS NULL OR e.id = :especialidadId)
          AND (:sedeId IS NULL OR EXISTS (
                SELECT 1 FROM ServicioSede ss
                WHERE ss.idServicio = s.id
                  AND ss.idSede = :sedeId
                  AND ss.activo = true
              ))
    """)
    List<Object[]> contarPorEstados(
        @Param("buscar") String buscar,
        @Param("especialidadId") Integer especialidadId,
        @Param("sedeId") Integer sedeId
    );
}