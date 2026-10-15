package com.neodent.legal.repository;

import com.neodent.legal.model.DocumentoLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, Long> {
    Optional<DocumentoLegal> findByCodigo(String codigo);
    List<DocumentoLegal> findByActivoTrueOrderByGrupoAscIdAsc();
    List<DocumentoLegal> findByOrderByGrupoAscIdAsc();
    boolean existsByCodigo(String codigo);
}
