package com.neodent.legal.repository;

import com.neodent.legal.model.AceptacionDocumentoLegal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AceptacionDocumentoLegalRepository extends JpaRepository<AceptacionDocumentoLegal, Long> {

    boolean existsByUsuarioIdAndVersionId(Long usuarioId, Long versionId);

    List<AceptacionDocumentoLegal> findByUsuarioId(Long usuarioId);

    @Query("SELECT a FROM AceptacionDocumentoLegal a " +
           "JOIN FETCH a.version v " +
           "JOIN FETCH v.documentoLegal d " +
           "JOIN FETCH a.usuario u " +
           "WHERE (:buscar IS NULL " +
           "   OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "   OR LOWER(v.version) LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "   OR LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :buscar, '%')))")
    Page<AceptacionDocumentoLegal> buscarAceptacionesPaginado(
        @Param("buscar") String buscar,
        Pageable pageable
    );
}
