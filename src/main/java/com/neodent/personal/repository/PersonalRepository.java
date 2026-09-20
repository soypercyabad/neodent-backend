package com.neodent.personal.repository;

import com.neodent.personal.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    Optional<Personal> findByUsuarioId(Long usuarioId);

    Optional<Personal> findByUsuarioIdAndActivoTrue(Long usuarioId);

    @Query("SELECT p FROM Personal p JOIN FETCH p.tipoDocumento td WHERE p.usuario.id IN :usuarioIds")
    List<Personal> findAllByUsuarioIdInWithTipoDocumento(@Param("usuarioIds") List<Long> usuarioIds);

    boolean existsByTipoDocumentoIdAndNumeroDocumentoIgnoreCase(Integer tipoDocumentoId, String numeroDocumento);

    boolean existsByTipoDocumentoIdAndNumeroDocumentoIgnoreCaseAndIdNot(Integer tipoDocumentoId, String numeroDocumento, Long id);
}