package com.neodent.legal.repository;

import com.neodent.legal.model.DocumentoLegalVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoLegalVersionRepository extends JpaRepository<DocumentoLegalVersion, Long> {

    List<DocumentoLegalVersion> findByActivoTrueOrderByFechaCreacionDesc();

    List<DocumentoLegalVersion> findByDocumentoLegalIdOrderByFechaCreacionDesc(Long idDocumentoLegal);

    Optional<DocumentoLegalVersion> findFirstByDocumentoLegalIdAndActivoTrueOrderByFechaCreacionDesc(Long idDocumentoLegal);

    Optional<DocumentoLegalVersion> findFirstByDocumentoLegalCodigoAndActivoTrueOrderByFechaCreacionDesc(String codigo);

    Optional<DocumentoLegalVersion> findByNombreArchivo(String nombreArchivo);

    boolean existsByDocumentoLegalIdAndVersion(Long idDocumentoLegal, String version);

    @Query("SELECT v FROM DocumentoLegalVersion v " +
           "JOIN FETCH v.documentoLegal d " +
           "WHERE (:buscar IS NULL OR LOWER(v.version) LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "   OR LOWER(v.nombreArchivo) LIKE LOWER(CONCAT('%', :buscar, '%')) " +
           "   OR LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
           "  AND (:activo IS NULL OR v.activo = :activo)")
    Page<DocumentoLegalVersion> buscarVersionesPaginado(
        @Param("buscar") String buscar,
        @Param("activo") Boolean activo,
        Pageable pageable
    );
}
