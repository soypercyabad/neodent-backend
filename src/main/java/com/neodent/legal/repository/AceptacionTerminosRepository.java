package com.neodent.legal.repository;

import com.neodent.legal.model.AceptacionTerminos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AceptacionTerminosRepository extends JpaRepository<AceptacionTerminos, Long> {
    boolean existsByUsuarioIdAndTerminosId(Long usuarioId, Long terminosId);
    Optional<AceptacionTerminos> findFirstByUsuarioIdOrderByAceptadoEnDesc(Long usuarioId);

    @Query("SELECT a FROM AceptacionTerminos a JOIN FETCH a.usuario JOIN FETCH a.terminos ORDER BY a.aceptadoEn DESC")
    List<AceptacionTerminos> findAllWithUsuarioAndTerminos();

    @Query(
        value = """
            SELECT a FROM AceptacionTerminos a
            JOIN FETCH a.usuario u
            JOIN FETCH a.terminos t
            LEFT JOIN Paciente pac ON pac.usuario.id = u.id
            LEFT JOIN Personal per ON per.usuario.id = u.id
            WHERE (:buscar IS NULL OR :buscar = '' OR
                   LOWER(COALESCE(pac.numeroDocumento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(per.numeroDocumento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')))
        """,
        countQuery = """
            SELECT COUNT(a) FROM AceptacionTerminos a
            JOIN a.usuario u
            JOIN a.terminos t
            LEFT JOIN Paciente pac ON pac.usuario.id = u.id
            LEFT JOIN Personal per ON per.usuario.id = u.id
            WHERE (:buscar IS NULL OR :buscar = '' OR
                   LOWER(COALESCE(pac.numeroDocumento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(per.numeroDocumento, '')) LIKE LOWER(CONCAT('%', :buscar, '%')))
        """
    )
    Page<AceptacionTerminos> buscarPaginado(
        @Param("buscar") String buscar,
        Pageable pageable
    );
}
